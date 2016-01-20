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

import org.sonar.runner.api.EmbeddedRunner;
import static org.sonarlint.cli.SonarProperties.*;
import org.sonar.runner.api.RunnerProperties;

import java.nio.file.Paths;
import java.util.Properties;

public class SonarLint {
  private EmbeddedRunner runner;
  private RunnerFactory runnerFactory;
  private boolean running;

  public SonarLint(RunnerFactory runnerFactory) {
    this.runnerFactory = runnerFactory;
  }

  public void start(Properties globalProps) {
    runner = runnerFactory.create(globalProps);
    runner.start();
    running = true;
  }

  public void runAnalysis(Properties analysisProps) {
    runner.runAnalysis(analysisProps);
  }

  public void stop() {
    runner.stop();
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

  public void validate(Properties props) {
    if (props.containsKey(RunnerProperties.HOST_URL)) {
      throw new IllegalStateException(String.format("Invalid property: '%s'. Can't set host with SonarLint", RunnerProperties.HOST_URL));
    }
    if (props.containsKey(ANALYSIS_MODE)) {
      throw new IllegalStateException(String.format("Invalid property: '%s'. Can't set analysis mode with SonarLint", ANALYSIS_MODE));
    }
    if (props.containsKey(ANALYSIS_MODE)) {
      throw new IllegalStateException(String.format("Invalid property: '%s'. Can't set analysis mode with SonarLint", ANALYSIS_MODE));
    }
  }

  public void setDefaults(Properties props, boolean json) {
    props.setProperty(ANALYSIS_MODE, "issues");

    if (props.containsKey(TEST_HOST_URL)) {
      props.setProperty(TEST_HOST_URL, props.getProperty(TEST_HOST_URL));
      props.remove(TEST_HOST_URL);
    } else {
      props.setProperty(RunnerProperties.HOST_URL, DEFAULT_HOST_URL);
    }

    setDefault(props, SOURCES, "");
    setDefault(props, PROJECT_KEY, getProjectName(props));
    setDefault(props, PROJECT_NAME, getProjectName(props));
    setDefault(props, PROJECT_VERSION, DEFAULT_VERSION);
    
    if(json) {
      props.setProperty(JSON_REPORT_ENABLE, Boolean.toString(true));
    }
    props.setProperty(CONSOLE_REPORT_ENABLE, Boolean.toString(true));
    props.setProperty(HTML_REPORT_ENABLE, Boolean.toString(true));
  }

  private static String getProjectName(Properties props) {
    if (props.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      return Paths.get(props.getProperty(PROPERTY_PROJECT_BASEDIR)).getFileName().toString();
    } else {
      return Paths.get("").toAbsolutePath().getFileName().toString();
    }
  }

  private static void setDefault(Properties props, String key, String defaultValue) {
    if (!props.contains(key)) {
      props.setProperty(key, defaultValue);
      Logger.get().info(String.format("Setting default value %s=%s", key, defaultValue));
    }
  }
}
