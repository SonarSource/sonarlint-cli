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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssuesReportTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private IssuesReport report;

  @Before
  public void setUp() {
    report = new IssuesReport(Paths.get(""), StandardCharsets.UTF_8);
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
  public void testAdd() {
    report.addIssue(createTestIssue("comp", "rule1", "name1", "MAJOR", 10));
    report.addIssue(createTestIssue("comp", "rule2", "name2", "MAJOR", 11));
    assertThat(report.getSummary()).isNotNull();
    assertThat(report.getSummary().getTotal()).isEqualTo(new IssueVariation(2, 0, 0));

    assertThat(report.getResourceReportsByResource()).containsOnlyKeys(Paths.get("comp"));
    assertThat(report.getRuleName("rule1")).isEqualTo("name1");
  }

  @Test
  public void testHtmlDecoratorFullLine() throws Exception {
    Path file = temp.newFile().toPath();
    FileUtils.write(file.toFile(), "if (a && b)\nif (a < b)\nif (a > b)", StandardCharsets.UTF_8);
    report.addIssue(createTestIssue(file.toString(), "rule1", "name1", "MAJOR", 1));
    report.addIssue(createTestIssue(file.toString(), "rule2", "name2", "MAJOR", 2));
    assertThat(report.getEscapedSource(file)).containsExactly("<span class=\"issue-0\">if (a &amp;&amp; b)</span>", "<span class=\"issue-1\">if (a &lt; b)</span>",
      "if (a &gt; b)");
  }

  @Test
  public void testHtmlDecoratorPreciseLocation() throws Exception {
    Path file = temp.newFile().toPath();
    FileUtils.write(file.toFile(), " foo bar ", StandardCharsets.UTF_8);
    Issue issue1 = createTestIssue(file.toString(), "rule1", "name1", "MAJOR", 1);
    when(issue1.getStartLineOffset()).thenReturn(1);
    when(issue1.getEndLineOffset()).thenReturn(8);
    Issue issue2 = createTestIssue(file.toString(), "rule2", "name2", "MAJOR", 1);
    when(issue2.getStartLineOffset()).thenReturn(5);
    when(issue2.getEndLineOffset()).thenReturn(8);
    report.addIssue(issue1);
    report.addIssue(issue2);
    assertThat(report.getEscapedSource(file)).containsExactly(" <span class=\"issue-0\">foo <span class=\"issue-1\">bar</span></span> ");
  }

  private static Issue createTestIssue(String filePath, String ruleKey, String name, String severity, int line) {
    ClientInputFile inputFile = mock(ClientInputFile.class);
    when(inputFile.getPath()).thenReturn(Paths.get(filePath));

    Issue issue = mock(Issue.class);
    when(issue.getStartLine()).thenReturn(line);
    when(issue.getStartLineOffset()).thenReturn(null);
    when(issue.getEndLine()).thenReturn(line);
    when(issue.getEndLineOffset()).thenReturn(null);
    when(issue.getRuleName()).thenReturn(name);
    when(issue.getInputFile()).thenReturn(inputFile);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getSeverity()).thenReturn(severity);
    return issue;
  }
}
