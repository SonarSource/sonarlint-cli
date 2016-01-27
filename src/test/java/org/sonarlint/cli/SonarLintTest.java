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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.IssueListener;
import org.sonarlint.cli.report.ReportFactory;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SonarLintTest {
  private RunnerFactory factory;
  private ReportFactory reportFactory;
  private SonarLint sonarLint;
  private EmbeddedRunner runner;
  private Properties props;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    props = new Properties();
    props.setProperty(SonarProperties.PROPERTY_PROJECT_BASEDIR, "");
    reportFactory = mock(ReportFactory.class);
    runner = mock(EmbeddedRunner.class);
    factory = mock(RunnerFactory.class);
    when(factory.create(any(Properties.class))).thenReturn(runner);
    sonarLint = new SonarLint(factory);
  }

  @Test
  public void testValidationHost() {
    props.put("sonar.host.url", "my.host");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Can't set host with SonarLint");
    sonarLint.validate(props);
  }

  @Test
  public void testAnalysis() {
    assertThat(sonarLint.isRunning()).isFalse();

    sonarLint.setDefaults(props);
    sonarLint.start(props);
    assertThat(sonarLint.isRunning()).isTrue();
    verify(factory).create(props);
    verify(runner).start();

    sonarLint.runAnalysis(props, reportFactory);
    verify(runner).runAnalysis(any(Properties.class), any(IssueListener.class));
    assertThat(sonarLint.isRunning()).isTrue();

    sonarLint.stop();
    verify(runner).stop();
    assertThat(sonarLint.isRunning()).isFalse();

    verifyNoMoreInteractions(runner);
    verifyNoMoreInteractions(factory);
  }

  @Test
  public void testValidationMode() {
    props.put("sonar.analysis.mode", "issues");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Can't set analysis mode with SonarLint");
    sonarLint.validate(props);
  }

  @Test
  public void testDefaults() {
    props.put("sonar.projectBaseDir", "/home/myproject");
    sonarLint.setDefaults(props);

    assertThat(props).containsEntry("sonar.host.url", "https://update.sonarlint.org");
    assertThat(props).containsEntry("sonar.analysis.mode", "issues");
    assertThat(props).containsEntry("sonar.sources", ".");
    assertThat(props).containsEntry("sonar.tests", ".");
    assertThat(props).containsEntry("sonar.test.inclusions", "**/*Test.*,**/test/**/*");
    assertThat(props).containsEntry("sonar.projectKey", "myproject");
    assertThat(props).containsEntry("sonar.projectName", "myproject");
    assertThat(props).containsEntry("sonar.projectVersion", "1.0");

    assertThat(props).containsEntry("sonar.issuesReport.console.enable", "false");
    assertThat(props).containsEntry("sonar.issuesReport.html.enable", "false");
    assertThat(props).doesNotContainKey("sonar.issuesReport.json.enable");
  }

  @Test
  public void testHostUrl() {
    props.put(SonarProperties.TEST_HOST_URL, "myhost");
  }

  @Test
  public void testDefaultWithPreviousValue() {
    props.put("sonar.projectBaseDir", "/home/myproject");
    props.put("sonar.projectVersion", "2.0");

    sonarLint.setDefaults(props);
    assertThat(props).containsEntry("sonar.projectVersion", "2.0");

  }

  @Test
  public void testValidation() {
    props.put("sonar.key", "value");
    sonarLint.validate(props);
  }
}
