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

public class SonarProperties {
  public static final String PROPERTY_ANALYSIS_MODE = "sonar.analysis.mode";
  public static final String PROPERTY_PROJECT_KEY = "sonar.projectKey";
  public static final String PROPERTY_PROJECT_VERSION = "sonar.projectVersion";
  public static final String PROPERTY_PROJECT_NAME = "sonar.projectName";
  public static final String PROPERTY_SOURCES = "sonar.sources";
  public static final String PROPERTY_TESTS = "sonar.tests";
  public static final String PROPERTY_TESTS_INCLUSIONS = "sonar.test.inclusions";
  public static final String PROPERTY_PROJECT_BASEDIR = "sonar.projectBaseDir";
  public static final String PROPERTY_HTML_REPORT_ENABLE = "sonar.issuesReport.html.enable";
  public static final String PROPERTY_CONSOLE_REPORT_ENABLE = "sonar.issuesReport.console.enable";
  public static final String PROPERTY_PROJECT_CONFIG_FILE = "sonar.projectConfigFile";

  public static final String TEST_HOST_URL = "sonar.test.url";
  public static final String SONARLINT_HOME = "sonarlint.home";
  public static final String PROJECT_HOME = "project.home";

  public static final String SONAR_PROJECT_PROPERTIES_FILENAME = "sonarlint.properties";

  public static final String DEFAULT_HOST_URL = "https://update.sonarlint.org";
  public static final String DEFAULT_VERSION = "1.0";
  public static final String DEFAULT_SOURCES = ".";
  public static final String DEFAULT_TESTS = ".";
  public static final String DEFAULT_TESTS_INCLUSIONS = "**/*Test.*,**/test/**/*";
}
