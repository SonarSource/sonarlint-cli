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
package org.sonarlint.cli.report;

import org.junit.Before;
import org.junit.Test;
import org.sonar.runner.api.Issue;
import org.sonarlint.cli.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsoleReportTest {
  private final static String PROJECT_NAME = "project";
  private final static Date DATE = new Date(System.currentTimeMillis());
  private ConsoleReport report;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private PrintStream stdOut;
  private PrintStream stdErr;

  @Before
  public void setUp() {
    setStreams();
    report = new ConsoleReport();
  }

  @Test
  public void testLog() throws IOException {
    List<Issue> issues = new LinkedList<>();
    issues.add(createTestIssue("comp1", "rule", "MAJOR", 10));
    issues.add(createTestIssue("comp1", "rule", "MINOR", 10));
    report.execute(PROJECT_NAME, DATE, issues);

    stdOut.flush();
    assertThat(getLog(out)).contains("Issues Report");
    assertThat(getLog(out)).contains("2 issues");
    assertThat(getLog(out)).contains("1 major");
    assertThat(getLog(out)).contains("1 minor");
    assertThat(getLog(out)).doesNotContain("new");
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

  private static Issue createTestIssue(String componentKey, String ruleKey, String severity, int line) {
    return Issue.builder()
      .setStartLine(line)
      .setComponentKey(componentKey)
      .setRuleKey(ruleKey)
      .setSeverity(severity)
      .setNew(false)
      .build();
  }
}
