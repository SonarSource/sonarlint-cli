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

import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.Issue;
import org.sonar.runner.api.IssueListener;
import org.sonar.runner.api.RunnerProperties;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.report.Reporter;
import org.sonarlint.cli.util.Logger;

import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.sonarlint.cli.SonarProperties.*;

public class SonarLint {
  private EmbeddedRunner runner;
  private RunnerFactory runnerFactory;
  private boolean running;
  private Properties global;

  public SonarLint(RunnerFactory runnerFactory) {
    this.runnerFactory = runnerFactory;
  }

  public void start(Properties globalProps) {
    this.global = new Properties();
    this.global.putAll(globalProps);

    runner = runnerFactory.create(globalProps);
    runner.start();
    running = true;
  }

  public void runAnalysis(Properties analysisProps, ReportFactory reportFactory) {
    Date start = new Date();
    String projectName = getFallback(analysisProps, global, PROPERTY_PROJECT_NAME);
    String baseDir = getFallback(analysisProps, global, PROPERTY_PROJECT_BASEDIR);

    IssueCollector collector = new IssueCollector();
    runner.runAnalysis(analysisProps, collector);
    generateReports(collector.get(), reportFactory, projectName, baseDir, start);
  }

  private static void generateReports(List<Issue> issues, ReportFactory reportFactory, String projectName, String baseDir, Date date) {
    List<Reporter> reporters = reportFactory.createReporters(baseDir);

    for (Reporter r : reporters) {
      r.execute(projectName, date, issues);
    }
  }

  private String getFallback(Properties p1, Properties p2, String key) {
    String v = p1.getProperty(key);
    if (v != null) {
      return v;
    }
    v = p2.getProperty(key);
    if (v != null) {
      return v;
    }
    throw new IllegalStateException("No value for key: " + key);
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
    if (props.containsKey(PROPERTY_ANALYSIS_MODE)) {
      throw new IllegalStateException(String.format("Invalid property: '%s'. Can't set analysis mode with SonarLint", PROPERTY_ANALYSIS_MODE));
    }
  }

  public void setDefaults(Properties props) {
    props.setProperty(PROPERTY_ANALYSIS_MODE, "issues");

    if (props.containsKey(TEST_HOST_URL)) {
      props.setProperty(RunnerProperties.HOST_URL, props.getProperty(TEST_HOST_URL));
      props.remove(TEST_HOST_URL);
    } else {
      props.setProperty(RunnerProperties.HOST_URL, DEFAULT_HOST_URL);
    }

    setDefault(props, PROPERTY_PROJECT_BASEDIR, Paths.get("").toAbsolutePath().toString());
    setDefault(props, PROPERTY_SOURCES, DEFAULT_SOURCES);
    setDefault(props, PROPERTY_TESTS, DEFAULT_TESTS);
    setDefault(props, PROPERTY_TESTS_INCLUSIONS, DEFAULT_TESTS_INCLUSIONS);
    setDefault(props, PROPERTY_PROJECT_KEY, getProjectName(props));
    setDefault(props, PROPERTY_PROJECT_NAME, getProjectName(props));
    setDefault(props, PROPERTY_PROJECT_VERSION, DEFAULT_VERSION);

    props.setProperty(PROPERTY_CONSOLE_REPORT_ENABLE, Boolean.toString(false));
    props.setProperty(PROPERTY_HTML_REPORT_ENABLE, Boolean.toString(false));
  }

  private static String getProjectName(Properties props) {
    if (props.containsKey(PROPERTY_PROJECT_BASEDIR)) {
      return Paths.get(props.getProperty(PROPERTY_PROJECT_BASEDIR)).getFileName().toString();
    } else {
      return Paths.get("").toAbsolutePath().getFileName().toString();
    }
  }

  private static void setDefault(Properties props, String key, String defaultValue) {
    if (!props.containsKey(key)) {
      props.setProperty(key, defaultValue);
      Logger.get().debug(String.format("Setting default value %s=%s", key, defaultValue));
    }
  }

  private static class IssueCollector implements IssueListener {
    private List<Issue> issues = new LinkedList<>();

    @Override
    public void handle(Issue issue) {
      issues.add(issue);
    }

    public List<Issue> get() {
      return issues;
    }
  }
}
