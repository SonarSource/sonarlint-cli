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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.tracking.IssueTrackable;
import org.sonarsource.sonarlint.core.tracking.Trackable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlReportTest {
  private HtmlReport html;
  private AnalysisResults result;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private Path reportFile;

  @Before
  public void setUp() {
    result = mock(AnalysisResults.class);
    when(result.fileCount()).thenReturn(1);
    reportFile = temp.getRoot().toPath().resolve("report.html");
    html = new HtmlReport(temp.getRoot().toPath(), reportFile, StandardCharsets.UTF_8);
  }

  @Test
  public void testHtml() {
    html.execute("project", new Date(), new LinkedList<>(), result, k -> null);
  }

  @Test
  public void testCopyRuleDesc() {
    html.execute("project", new Date(), Arrays.asList(createTestIssue("foo", "squid:1234", "bla", "MAJOR", 1)), result,
      k -> "squid:1234".equals(k) ? mockRuleDetails() : null);

    assertThat(reportFile.getParent().resolve("sonarlintreport_rules/rule.css").toFile()).exists();
    assertThat(reportFile.getParent().resolve("sonarlintreport_rules/squid_1234.html").toFile()).usingCharset(StandardCharsets.UTF_8).hasContent(
      "<!doctype html><html><head><link href=\"rule.css\" rel=\"stylesheet\" type=\"text/css\" /></head><body><h1><big>Foo</big> (squid:1234)</h1><div class=\"rule-desc\">foo bar</div></body></html>");
  }

  @Test
  public void testExtendedDesc() {
    RuleDetails mockRuleDetailsWithExtendedDesc = mockRuleDetails();
    when(mockRuleDetailsWithExtendedDesc.getExtendedDescription()).thenReturn("bar baz");

    html.execute("project", new Date(), Arrays.asList(createTestIssue("foo", "squid:1234", "bla", "MAJOR", 1)), result,
      k -> "squid:1234".equals(k) ? mockRuleDetailsWithExtendedDesc : null);

    assertThat(reportFile.getParent().resolve("sonarlintreport_rules/rule.css").toFile()).exists();
    assertThat(reportFile.getParent().resolve("sonarlintreport_rules/squid_1234.html").toFile()).usingCharset(StandardCharsets.UTF_8).hasContent(
      "<!doctype html><html><head><link href=\"rule.css\" rel=\"stylesheet\" type=\"text/css\" /></head><body><h1><big>Foo</big> (squid:1234)</h1><div class=\"rule-desc\">foo bar\n<div>bar baz</div></div></body></html>");
  }

  private RuleDetails mockRuleDetails() {
    RuleDetails ruleDetails = mock(RuleDetails.class);
    when(ruleDetails.getName()).thenReturn("Foo");
    when(ruleDetails.getHtmlDescription()).thenReturn("foo bar");
    when(ruleDetails.getExtendedDescription()).thenReturn("");
    return ruleDetails;
  }

  private static Trackable createTestIssue(String filePath, String ruleKey, String name, String severity, int line) {
    ClientInputFile inputFile = mock(ClientInputFile.class);
    when(inputFile.getPath()).thenReturn(filePath);

    Issue issue = mock(Issue.class);
    when(issue.getStartLine()).thenReturn(line);
    when(issue.getStartLineOffset()).thenReturn(null);
    when(issue.getEndLine()).thenReturn(line);
    when(issue.getEndLineOffset()).thenReturn(null);
    when(issue.getRuleName()).thenReturn(name);
    when(issue.getInputFile()).thenReturn(inputFile);
    when(issue.getRuleKey()).thenReturn(ruleKey);
    when(issue.getSeverity()).thenReturn(severity);
    return new IssueTrackable(issue);
  }
}
