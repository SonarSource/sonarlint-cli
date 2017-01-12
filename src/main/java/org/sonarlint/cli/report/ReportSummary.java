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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonarlint.cli.util.Util;

public class ReportSummary {

  private final IssueVariation total = new IssueVariation();

  private final Map<IssueCategory, CategoryReport> reportByCategory = new LinkedHashMap<>();
  private final Map<String, IssueVariation> totalByRuleKey = new LinkedHashMap<>();
  private final Map<String, IssueVariation> totalBySeverity = new LinkedHashMap<>();

  ReportSummary() {
  }

  public void addIssue(RichIssue issue) {
    Severity severity = Severity.create(issue.getSeverity());
    IssueCategory category = new IssueCategory(issue.getRuleKey(), severity, issue.getRuleName());

    IssueVariation byRuleKey = Util.getOrCreate(totalByRuleKey, issue.getRuleKey(), IssueVariation::new);
    CategoryReport byCategory = getOrCreate(reportByCategory, category);
    IssueVariation bySeverity = Util.getOrCreate(totalBySeverity, issue.getSeverity(), IssueVariation::new);

    total.incrementCountInCurrentAnalysis();
    byCategory.getTotal().incrementCountInCurrentAnalysis();
    byRuleKey.incrementCountInCurrentAnalysis();
    bySeverity.incrementCountInCurrentAnalysis();
  }

  public IssueVariation getTotal() {
    return total;
  }

  public Map<String, IssueVariation> getTotalBySeverity() {
    return totalBySeverity;
  }

  public Map<String, IssueVariation> getTotalByRuleKey() {
    return totalByRuleKey;
  }

  private static CategoryReport getOrCreate(Map<IssueCategory, CategoryReport> m, IssueCategory key) {
    CategoryReport report = m.get(key);
    if (report != null) {
      return report;
    }

    report = new CategoryReport(key);
    m.put(key, report);
    return report;
  }

  public List<CategoryReport> getCategoryReports() {
    List<CategoryReport> result = new ArrayList<>(reportByCategory.values());
    Collections.sort(result, new CategoryReportComparator());
    return result;
  }
}
