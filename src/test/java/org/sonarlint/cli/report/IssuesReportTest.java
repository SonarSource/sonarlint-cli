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

import java.nio.file.Paths;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesReportTest {
  private IssuesReport report;

  @Before
  public void setUp() {
    report = new IssuesReport(Paths.get(""));
  }

  @Test
  public void testRoundTrip() {
    Date d = new Date();
    String title = "title";

    report.setDate(d);
    report.setTitle(title);

    assertThat(report.getDate()).isEqualTo(d);
    assertThat(report.getTitle()).isEqualTo(title);
    assertThat(report.noFiles()).isTrue();

    report.setFilesAnalyzed(1);
    assertThat(report.getFilesAnalyzed()).isEqualTo(1);
    assertThat(report.noFiles()).isFalse();
  }

  @Test
  public void issueId() {
    Issue i1 = createTestIssue("comp", "rule1", "name1", "MAJOR", 10);
    Issue i2 = createTestIssue("comp", "rule2", "name2", "MAJOR", 11);
    report.addIssue(i1);
    report.addIssue(i2);

    assertThat(report.issueId(i1)).isEqualTo("issue0");
    assertThat(report.issueId(i2)).isEqualTo("issue1");
  }

  @Test
  public void testAdd() {
    report.addIssue(createTestIssue("comp", "rule1", "name1", "MAJOR", 10));
    report.addIssue(createTestIssue("comp", "rule2", "name2", "MAJOR", 11));
    assertThat(report.getSummary()).isNotNull();
    assertThat(report.getSummary().getTotal()).isEqualTo(new IssueVariation(2, 0, 0));

    assertThat(report.getResourceReportsByResource()).containsOnlyKeys(Paths.get("comp"));
    assertThat(report.getRuleName("rule1")).isEqualTo("name1");
  }

  private static Issue createTestIssue(String filePath, String ruleKey, String name, String severity, int line) {
    ClientInputFile inputFile = mock(ClientInputFile.class);
    when(inputFile.getPath()).thenReturn(Paths.get(filePath));

    Issue issue = mock(Issue.class);
    when(issue.getStartLine()).thenReturn(line);
    when(issue.getRuleName()).thenReturn(name);
    when(issue.getInputFile()).thenReturn(inputFile);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getSeverity()).thenReturn(severity);
    return issue;
  }
}
