/*
 * SonarLint CLI
 * Copyright (C) 2016 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Properties;

public class Main {
  static final int SUCCESS = 0;
  static final int ERROR = 1;

  private final Options opts;
  private final Logger logger;
  private final SonarLint sonarLint;
  private BufferedReader inputReader;

  public Main(Options opts, Logger logger, SonarLint sonarLint) {
    this.opts = opts;
    this.logger = logger;
    this.sonarLint = sonarLint;
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

    logger.setDebugEnabled(opts.isVerbose());
    logger.setDisplayStackTrace(opts.showStack());

    SystemInfo.print(logger);

    if (opts.showStack()) {
      logger.info("Error stacktraces are turned on.");
    }

    Properties combinedProps = null;

    try {
      Conf conf = new Conf(opts.properties());
      // these will be the props based on CLI, system props and configuration files
      combinedProps = conf.properties();
      sonarLint.validate(combinedProps);
      sonarLint.setDefaults(combinedProps, opts.jsonReport());
    } catch (IOException e) {
      logger.error("Error processing properties", e);
      return ERROR;
    }

    Stats stats = new Stats();
    try {
      if (opts.isInteractive()) {
        runInteractive(stats, combinedProps);
      } else {
        runOnce(stats, combinedProps);
      }
    } catch (Exception e) {
      displayExecutionResult(stats, "FAILURE");
      showError("Error during Sonar runner execution", e, opts.showStack(), opts.isVerbose());
      return ERROR;
    }

    return SUCCESS;
  }

  private void runOnce(Stats stats, Properties props) {
    stats.start();
    if (!sonarLint.isRunning()) {
      sonarLint.start(props);
    }
    sonarLint.runAnalysis(props);
    sonarLint.stop();
    displayExecutionResult(stats, "SUCCESS");
  }

  private void runInteractive(Stats stats, Properties props) throws IOException {
    do {
      stats.start();
      if (!sonarLint.isRunning()) {
        sonarLint.start(props);
      }
      sonarLint.runAnalysis(props);
      displayExecutionResult(stats, "SUCCESS");
    } while (waitForUser());

    sonarLint.stop();
  }

  private boolean waitForUser() throws IOException {
    if (inputReader == null) {
      inputReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }

    logger.info("");
    logger.info("<Press enter to restart analysis or Ctrl+C to exit the interactive mode>");
    String line = inputReader.readLine();

    return line != null;
  }

  public static void main(String[] args) {
    Logger logger = Logger.get();
    Options opts = null;
    try {
      opts = Options.parse(args);
    } catch (ParseException e) {
      logger.error("Error parsing arguments", e);
      System.exit(ERROR);
    }

    int ret = new Main(opts, logger, new SonarLint(new RunnerFactory(logger))).run();
    System.exit(ret);
  }

  private void displayExecutionResult(Stats stats, String resultMsg) {
    logger.info("------------------------------------------------------------------------");
    logger.info("EXECUTION " + resultMsg);
    logger.info("------------------------------------------------------------------------");
    stats.stop();
    logger.info("------------------------------------------------------------------------");
  }

  private void showError(String message, Throwable e, boolean showStackTrace, boolean debug) {
    if (showStackTrace) {
      logger.error(message, e);
      if (!debug) {
        logger.error("");
        suggestDebugMode();
      }
    } else {
      logger.error(message);
      if (e != null) {
        logger.error(e.getMessage());
        String previousMsg = "";
        for (Throwable cause = e.getCause(); cause != null
          && cause.getMessage() != null
          && !cause.getMessage().equals(previousMsg); cause = cause.getCause()) {
          logger.error("Caused by: " + cause.getMessage());
          previousMsg = cause.getMessage();
        }
      }
      logger.error("");
      logger.error("To see the full stack trace of the errors, re-run SonarLint with the -e switch.");
      if (!debug) {
        suggestDebugMode();
      }
    }
  }

  private void suggestDebugMode() {
    logger.error("Re-run SonarLint using the -X switch to enable full debug logging.");
  }
}
