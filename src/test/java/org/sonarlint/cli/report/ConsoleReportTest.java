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
package org.sonarlint.cli.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarlint.cli.TestUtils.createTestIssue;

public class ConsoleReportTest {
  private final static String PROJECT_NAME = "project";
  private final static Date DATE = new Date(System.currentTimeMillis());
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private ConsoleReport report;
  private AnalysisResults result;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private PrintStream stdOut;
  private PrintStream stdErr;

  @Before
  public void setUp() {
    setStreams();
    report = new ConsoleReport();
    result = mock(AnalysisResults.class);
    when(result.fileCount()).thenReturn(1);
  }

  @Test
  public void testLog() throws IOException {
    List<Issue> issues = new LinkedList<>();
    issues.add(createTestIssue("comp1", "rule", "MAJOR", 10));
    issues.add(createTestIssue("comp1", "rule", "MINOR", 10));
    issues.add(createTestIssue("comp1", "rule", "CRITICAL", 10));
    issues.add(createTestIssue("comp1", "rule", "INFO", 10));
    issues.add(createTestIssue("comp1", "rule", "BLOCKER", 10));

    report.execute(PROJECT_NAME, DATE, issues, result, k -> null);

    stdOut.flush();
    assertThat(getLog(out)).contains("SonarLint Report");
    assertThat(getLog(out)).contains("5 issues");
    assertThat(getLog(out)).contains("1 major");
    assertThat(getLog(out)).contains("1 minor");
    assertThat(getLog(out)).contains("1 info");
    assertThat(getLog(out)).contains("1 critical");
    assertThat(getLog(out)).contains("1 blocker");

    assertThat(getLog(out)).doesNotContain("new");
  }

  @Test
  public void testVerboseLog() throws IOException {

    ConsoleReport verboseReport = new ConsoleReport(true);

    List<Issue> issues = new LinkedList<>();
    issues.add(createTestIssue("comp1", "rule", "MAJOR", 20));
    issues.add(createTestIssue("comp1", "rule", "MINOR", 20));
    issues.add(createTestIssue("comp1", "rule", "CRITICAL", 20));
    issues.add(createTestIssue("comp1", "rule", "INFO", 20));
    issues.add(createTestIssue("comp1", "rule", "BLOCKER", 20));

    verboseReport.execute(PROJECT_NAME, DATE, issues, result, k -> null);

    stdOut.flush();
    assertThat(getLog(out)).contains("SonarLint Report");
    assertThat(getLog(out)).contains("5 issues");
    assertThat(getLog(out)).contains("1 major");
    assertThat(getLog(out)).contains("1 minor");
    assertThat(getLog(out)).contains("1 info");
    assertThat(getLog(out)).contains("1 critical");
    assertThat(getLog(out)).contains("1 blocker");

    assertThat(getLog(out)).contains("{comp1} {MAJOR} {20:0 - 0:0} {null}");
    assertThat(getLog(out)).contains("{comp1} {MINOR} {20:0 - 0:0} {null}");
    assertThat(getLog(out)).contains("{comp1} {CRITICAL} {20:0 - 0:0} {null}");
    assertThat(getLog(out)).contains("{comp1} {INFO} {20:0 - 0:0} {null}");
    assertThat(getLog(out)).contains("{comp1} {BLOCKER} {20:0 - 0:0} {null}");

    assertThat(getLog(out)).doesNotContain("new");
  }

  @Test
  public void testInvalidSeverity() throws IOException {
    List<Issue> issues = new LinkedList<>();
    issues.add(createTestIssue("comp1", "rule", "INVALID", 10));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unknown severity");
    report.execute(PROJECT_NAME, DATE, issues, result, k -> null);
  }

  @Test
  public void testReportWithoutIssues() throws IOException {
    List<Issue> issues = new LinkedList<>();
    report.execute(PROJECT_NAME, DATE, issues, result, k -> null);
    stdOut.flush();
    assertThat(getLog(out)).contains("SonarLint Report");
    assertThat(getLog(out)).contains("No issues to display");
    assertThat(getLog(out)).contains("1 file analyzed");
  }

  @Test
  public void testReportMultipleFiles() throws IOException {
    when(result.fileCount()).thenReturn(2);
    List<Issue> issues = new LinkedList<>();
    report.execute(PROJECT_NAME, DATE, issues, result, k -> null);
    stdOut.flush();
    assertThat(getLog(out)).contains("SonarLint Report");
    assertThat(getLog(out)).contains("No issues to display");
    assertThat(getLog(out)).contains("2 files analyzed");
  }

  @Test
  public void testReportNoFilesAnalyzed() throws IOException {
    List<Issue> issues = new LinkedList<>();
    when(result.fileCount()).thenReturn(0);
    report.execute(PROJECT_NAME, DATE, issues, result, k -> null);
    stdOut.flush();
    assertThat(getLog(out)).contains("SonarLint Report");
    assertThat(getLog(out)).contains("No files analyzed");

    assertThat(getLog(out)).doesNotContain("issues");
  }

  private String getLog(ByteArrayOutputStream byteStream) throws IOException {
    return new String(byteStream.toByteArray(), StandardCharsets.UTF_8);
  }

  private void setStreams() {
    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    stdOut = new PrintStream(out);
    stdErr = new PrintStream(err);
    Logger.set(stdOut, stdErr);
  }
}
