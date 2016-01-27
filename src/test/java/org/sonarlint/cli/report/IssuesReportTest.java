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
import org.sonarsource.sonarlint.core.IssueListener;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesReportTest {
  private IssuesReport report;

  @Before
  public void setUp() {
    report = new IssuesReport();
  }

  @Test
  public void testRoundTrip() {
    Date d = new Date();
    String title = "title";

    report.setDate(d);
    report.setTitle(title);

    assertThat(report.getDate()).isEqualTo(d);
    assertThat(report.getTitle()).isEqualTo(title);
  }

  @Test
  public void testAdd() {
    report.addIssue(createTestIssue("comp", "rule1", "MAJOR", 10));
    assertThat(report.getSummary()).isNotNull();
    assertThat(report.getSummary().getTotal()).isEqualTo(new IssueVariation(1, 0, 0));

    assertThat(report.getResourceReportsByResource()).containsOnlyKeys("comp");
  }

  private static IssueListener.Issue createTestIssue(String filePath, String ruleKey, String severity, int line) {
    return new IssueListener.Issue()
      .setStartLine(line)
      .setFilePath(Paths.get(filePath))
      .setRuleKey(ruleKey)
      .setSeverity(severity);
  }
}
