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
package org.sonarlint.cli.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.tracking.IssueTrackable;
import org.sonarsource.sonarlint.core.tracking.Trackable;

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
  public void test_round_trip() {
    Date d = new Date();
    String title = "title";

    report.setDate(d);
    report.setTitle(title);

    assertThat(report.getDate()).isEqualTo(d);
    assertThat(report.getTitle()).isEqualTo(title);
    assertThat(report.noIssues()).isTrue();
    assertThat(report.noFiles()).isTrue();

    report.setFilesAnalyzed(1);
    assertThat(report.getFilesAnalyzed()).isEqualTo(1);
    assertThat(report.noFiles()).isFalse();
  }

  @Test
  public void should_find_added_issue() {
    String filePath = "comp";
    String ruleKey = "rule1";
    report.addIssue(createTestIssue(filePath, ruleKey, "name1", "MAJOR", 10));
    report.addIssue(createTestIssue(filePath, "rule2", "name2", "MAJOR", 11));
    assertThat(report.getSummary()).isNotNull();
    assertThat(report.getSummary().getTotal()).isEqualTo(new IssueVariation(2, 0, 0));

    assertThat(report.getResourceReportsByResource()).containsOnlyKeys(Paths.get(filePath));
    assertThat(report.getRuleName(ruleKey)).isEqualTo("name1");

    assertThat(report.noIssues()).isFalse();
    assertThat(report.getResourceReports()).isNotEmpty();
    assertThat(report.getResourcesWithReport()).isNotEmpty();
  }

  @Test
  public void should_decorate_full_line_when_no_precise_location() throws Exception {
    Path file = temp.newFile().toPath();
    FileUtils.write(file.toFile(), "if (a && b)\nif (a < b)\nif (a > b)", StandardCharsets.UTF_8);
    report.addIssue(createTestIssue(file.toString(), "rule1", "name1", "MAJOR", 1));
    report.addIssue(createTestIssue(file.toString(), "rule2", "name2", "MAJOR", 2));
    assertThat(report.getEscapedSource(file)).containsExactly("<span class=\"issue-0\">if (a &amp;&amp; b)</span>", "<span class=\"issue-1\">if (a &lt; b)</span>",
      "if (a &gt; b)");
  }

  @Test
  public void should_decorate_precise_location() throws Exception {
    Path file = temp.newFile().toPath();
    FileUtils.write(file.toFile(), " foo bar ", StandardCharsets.UTF_8);
    Trackable issue1 = createTestIssue(file.toString(), "rule1", "name1", "MAJOR", 1);
    when(issue1.getIssue().getStartLineOffset()).thenReturn(1);
    when(issue1.getIssue().getEndLineOffset()).thenReturn(8);
    Trackable issue2 = createTestIssue(file.toString(), "rule2", "name2", "MAJOR", 1);
    when(issue2.getIssue().getStartLineOffset()).thenReturn(5);
    when(issue2.getIssue().getEndLineOffset()).thenReturn(8);
    report.addIssue(issue1);
    report.addIssue(issue2);
    assertThat(report.getEscapedSource(file)).containsExactly(" <span class=\"issue-0\">foo <span class=\"issue-1\">bar</span></span> ");
  }

  @Test
  public void should_be_able_to_create_issue_without_file() {
    Trackable issueWithoutFile = createTestIssue(null, "rule1", "name1", "MAJOR", 1);
    report.addIssue(issueWithoutFile);
    assertThat(report.getSummary().getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
  }

  @Test
  public void should_return_empty_escaped_source_for_null_path() {
    assertThat(report.getEscapedSource(null)).isEmpty();
  }

  @Test
  public void should_return_empty_escaped_source_for_nonexistent_file() {
    assertThat(report.getEscapedSource(Paths.get("nonexistent"))).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void getEscapedSource_should_throw_on_unreadable_file() throws IOException {
    report.getEscapedSource(temp.newFolder().toPath());
  }

  @Test(expected = IllegalStateException.class)
  public void getEscapedSource_should_throw_if_file_has_no_associated_report() throws IOException {
    Path file = temp.newFile().toPath();
    FileUtils.write(file.toFile(), "blah\nblah\n", StandardCharsets.UTF_8);
    report.getEscapedSource(file);
  }

  private static Trackable createTestIssue(@Nullable String filePath, String ruleKey, String name, String severity, int line) {
    Issue issue = mock(Issue.class);

    if (filePath != null) {
      ClientInputFile inputFile = mock(ClientInputFile.class);
      when(inputFile.getPath()).thenReturn(filePath);
      when(issue.getInputFile()).thenReturn(inputFile);
    }

    when(issue.getStartLine()).thenReturn(line);
    when(issue.getStartLineOffset()).thenReturn(null);
    when(issue.getEndLine()).thenReturn(line);
    when(issue.getEndLineOffset()).thenReturn(null);
    when(issue.getRuleName()).thenReturn(name);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getSeverity()).thenReturn(severity);
    return new IssueTrackable(issue);
  }
}
