/*
 * SonarLint CLI
 * Copyright (C) 2016-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Map;
import org.sonarlint.cli.analysis.SonarLint;
import org.sonarlint.cli.analysis.SonarLintFactory;
import org.sonarlint.cli.config.ConfigurationReader;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.util.Logger;
import org.sonarlint.cli.util.System2;
import org.sonarlint.cli.util.SystemInfo;
import org.sonarlint.cli.util.Util;

import static org.sonarlint.cli.SonarProperties.PROJECT_HOME;

public class Main {
  static final int SUCCESS = 0;
  static final int ERROR = 1;

  private static final Logger LOGGER = Logger.get();

  private final Options opts;
  private final ReportFactory reportFactory;
  private BufferedReader inputReader;
  private final InputFileFinder fileFinder;
  private final Path projectHome;
  private final SonarLintFactory sonarLintFactory;

  public Main(Options opts, SonarLintFactory sonarLintFactory, ReportFactory reportFactory, InputFileFinder fileFinder, Path projectHome) {
    this.opts = opts;
    this.sonarLintFactory = sonarLintFactory;
    this.reportFactory = reportFactory;
    this.fileFinder = fileFinder;
    this.projectHome = projectHome;
  }

  int run() {
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
      SonarLint sonarLint = sonarLintFactory.createSonarLint(projectHome, opts.isUpdate(), opts.isVerbose());
      sonarLint.start(opts.isUpdate());

      Map<String, String> props = Util.toMap(opts.properties());

      if (opts.isInteractive()) {
        runInteractive(stats, sonarLint, props, projectHome);
      } else {
        runOnce(stats, sonarLint, props, projectHome);
      }
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error executing SonarLint", e, opts.showStack(), opts.isVerbose());
      return ERROR;
    }

    return SUCCESS;
  }

  private static Path getProjectHome(System2 system) {
    String projectHome = system.getProperty(PROJECT_HOME);
    if (projectHome == null) {
      throw new IllegalStateException("Can't find project home. System property not set: " + PROJECT_HOME);
    }
    return Paths.get(projectHome);
  }

  private void runOnce(Stats stats, SonarLint sonarLint, Map<String, String> props, Path projectHome) throws IOException {
    stats.start();
    sonarLint.runAnalysis(props, reportFactory, fileFinder, projectHome);
    sonarLint.stop();
    displayExecutionResult(stats, "SUCCESS");
  }

  private void runInteractive(Stats stats, SonarLint sonarLint, Map<String, String> props, Path projectHome) throws IOException {
    do {
      stats.start();
      sonarLint.runAnalysis(props, reportFactory, fileFinder, projectHome);
      displayExecutionResult(stats, "SUCCESS");
    } while (waitForUser());

    sonarLint.stop();
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
    execute(args, System2.INSTANCE);
  }

  @VisibleForTesting
  static void execute(String[] args, System2 system) {
    Options parsedOpts;
    try {
      parsedOpts = Options.parse(args);
    } catch (ParseException e) {
      LOGGER.error("Error parsing arguments: " + e.getMessage(), e);
      Options.printUsage();
      system.exit(ERROR);
      return;
    }

    Charset charset;
    try {
      if (parsedOpts.charset() != null) {
        charset = Charset.forName(parsedOpts.charset());
      } else {
        charset = Charset.defaultCharset();
      }
    } catch (Exception e) {
      LOGGER.error("Error creating charset: " + parsedOpts.charset(), e);
      system.exit(ERROR);
      return;
    }

    InputFileFinder fileFinder = new InputFileFinder(parsedOpts.src(), parsedOpts.tests(), parsedOpts.exclusions(), charset);
    ReportFactory reportFactory = new ReportFactory(charset);
    ConfigurationReader reader = new ConfigurationReader();
    SonarLintFactory sonarLintFactory = new SonarLintFactory(reader);

    int ret = new Main(parsedOpts, sonarLintFactory, reportFactory, fileFinder, getProjectHome(system)).run();
    system.exit(ret);
    return;
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
      LOGGER.error(e.getMessage());
      String previousMsg = "";
      for (Throwable cause = e.getCause(); cause != null
        && cause.getMessage() != null
        && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
        LOGGER.error("Caused by: " + cause.getMessage());
        previousMsg = cause.getMessage();
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
