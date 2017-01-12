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

import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonarlint.cli.TestUtils.createTestIssue;

public class ReportSummaryTest {
  private ReportSummary summary;

  @Before
  public void setUp() {
    summary = new ReportSummary();
  }

  @Test
  public void test() {
    for (RichIssue i : createTestIssues()) {
      summary.addIssue(i);
    }

    assertThat(summary.getTotalByRuleKey()).contains(
      entry("rule1", variation(2, 0, 0)),
      entry("rule2", variation(2, 0, 0)));

    assertThat(summary.getTotalBySeverity()).contains(
      entry("MAJOR", variation(2, 0, 0)),
      entry("MINOR", variation(1, 0, 0)),
      entry("BLOCKER", variation(1, 0, 0)));

    assertThat(summary.getCategoryReports()).hasSize(3);
    assertVar(summary.getTotal(), 4, 0, 0);
  }

  private static IssueVariation variation(int current, int newCount, int resolved) {
    return new IssueVariation(current, newCount, resolved);
  }

  private static void assertVar(IssueVariation iv, int current, int newCount, int resolved) {
    assertThat(iv.getCountInCurrentAnalysis()).isEqualTo(current);
    assertThat(iv.getNewIssuesCount()).isEqualTo(newCount);
    assertThat(iv.getResolvedIssuesCount()).isEqualTo(resolved);
  }

  private static List<RichIssue> createTestIssues() {
    List<RichIssue> issueList = new LinkedList<>();

    issueList.add(createTestIssue("comp1", "rule1", "MAJOR", 10));
    issueList.add(createTestIssue("comp1", "rule2", "MINOR", 11));
    issueList.add(createTestIssue("comp4", "rule1", "MAJOR", 12));
    issueList.add(createTestIssue("comp2", "rule2", "BLOCKER", 13));

    return issueList;
  }

}
