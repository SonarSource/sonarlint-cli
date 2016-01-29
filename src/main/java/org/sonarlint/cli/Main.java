/*
 * SonarLint CLI
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.util.Logger;
import org.sonarlint.cli.util.SystemInfo;

public class Main {
  static final int SUCCESS = 0;
  static final int ERROR = 1;

  private static final Logger LOGGER = Logger.get();
  
  private final Options opts;
  private final ReportFactory reportFactory;
  private BufferedReader inputReader;
  private final SonarLint sonarlint;
  private final InputFileFinder fileFinder;

  public Main(Options opts, SonarLint sonarlint, ReportFactory reportFactory, InputFileFinder fileFinder) {
    this.opts = opts;
    this.sonarlint = sonarlint;
    this.reportFactory = reportFactory;
    this.fileFinder = fileFinder;
  }

  public int run() {
    if (opts.isHelp()) {
      Options.printUsage();
      return SUCCESS;
    }

    if (opts.isVersion()) {
      Logger.get().info(SystemInfo.getVersion());
      return SUCCESS;
    }

    reportFactory.setHtmlPath(opts.htmlReport());

    LOGGER.setDebugEnabled(opts.isVerbose());
    LOGGER.setDisplayStackTrace(opts.showStack());

    SystemInfo.print(LOGGER);

    if (opts.showStack()) {
      LOGGER.info("Error stacktraces are turned on.");
    }

    Stats stats = new Stats();
    try {
      if (opts.isInteractive()) {
        runInteractive(stats, sonarlint, opts);
      } else {
        runOnce(stats, sonarlint, opts);
      }
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error executing SonarLint", e, opts.showStack(), opts.isVerbose());
      return ERROR;
    }

    return SUCCESS;
  }

  private void runOnce(Stats stats, SonarLint sonarlint, Options options) throws IOException {
    stats.start();
    if (!sonarlint.isRunning()) {
      sonarlint.start();
    }
    sonarlint.runAnalysis(options, reportFactory, fileFinder);
    sonarlint.stop();
    displayExecutionResult(stats, "SUCCESS");
  }

  private void runInteractive(Stats stats, SonarLint sonarlint, Options options) throws IOException {
    do {
      stats.start();
      if (!sonarlint.isRunning()) {
        sonarlint.start();
      }
      sonarlint.runAnalysis(options, reportFactory, fileFinder);
      displayExecutionResult(stats, "SUCCESS");
    } while (waitForUser());

    sonarlint.stop();
  }

  private boolean waitForUser() throws IOException {
    if (inputReader == null) {
      inputReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
    LOGGER.info("");
    LOGGER.info("<Press enter to restart analysis or Ctrl+C to exit the interactive mode>");
    String line = inputReader.readLine();
    return line != null;
  }

  public void setIn(InputStream in) {
    inputReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  public static void main(String[] args) {
    Options opts = null;
    try {
      opts = Options.parse(args);
    } catch (ParseException e) {
      LOGGER.error("Error parsing arguments", e);
      System.exit(ERROR);
    }

    Charset charset = null;
    try {
      if (opts.charset() != null) {
        charset = Charset.forName(opts.charset());
      } else {
        charset = Charset.defaultCharset();
      }
    } catch (Exception e) {
      LOGGER.error("Error creating charset: " + opts.charset(), e);
    }

    InputFileFinder fileFinder = new InputFileFinder(opts.src(), opts.tests(), charset);
    ReportFactory reportFactory = new ReportFactory(charset);
    SonarLint sonarlint = null;
    try {
      sonarlint = new SonarLint(opts);
    } catch (IOException e) {
      LOGGER.error("Error loading plugins", e);
      System.exit(ERROR);
    }

    int ret = new Main(opts, sonarlint, reportFactory, fileFinder).run();
    System.exit(ret);
  }

  private static void displayExecutionResult(Stats stats, String resultMsg) {
    String dashes = "------------------------------------------------------------------------";
    LOGGER.info(dashes);
    LOGGER.info("EXECUTION " + resultMsg);
    LOGGER.info(dashes);
    stats.stop();
    LOGGER.info(dashes);
  }

  private static void showError(String message, Throwable e, boolean showStackTrace, boolean debug) {
    if (showStackTrace) {
      LOGGER.error(message, e);
      if (!debug) {
        LOGGER.error("");
        suggestDebugMode();
      }
    } else {
      LOGGER.error(message);
      if (e != null) {
        LOGGER.error(e.getMessage());
        String previousMsg = "";
        for (Throwable cause = e.getCause(); cause != null
          && cause.getMessage() != null
          && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
          LOGGER.error("Caused by: " + cause.getMessage());
          previousMsg = cause.getMessage();
        }
      }
      LOGGER.error("");
      LOGGER.error("To see the full stack trace of the errors, re-run SonarLint with the -e switch.");
      if (!debug) {
        suggestDebugMode();
      }
    }
  }

  private static void suggestDebugMode() {
    LOGGER.error("Re-run SonarLint using the -X switch to enable full debug logging.");
  }
}
