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

import org.sonar.runner.api.Issue;
import org.sonarlint.cli.util.Function;
import org.sonarlint.cli.util.MutableInt;

import static org.sonarlint.cli.util.Util.getOrCreate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class ResourceReport {
  private final String resource;
  private final boolean project = false;
  private final IssueVariation total = new IssueVariation();
  private final List<Issue> issues = new ArrayList<>();

  private final Map<IssueCategory, CategoryReport> reportByCategory = new HashMap<>();

  private final Map<Integer, List<Issue>> issuesPerLine = new HashMap<>();
  private final Map<Integer, List<Issue>> newIssuesPerLine = new HashMap<>();

  private final Map<String, MutableInt> issuesByRule = new HashMap<>();
  private final Map<Severity, MutableInt> issuesBySeverity = new HashMap<>();

  ResourceReport(String resource) {
    this.resource = resource;
  }

  public String getResourceNode() {
    return resource;
  }

  public String getName() {
    return resource;
  }

  public String getType() {
    if (project) {
      return "PRJ";
    } else {
      return "FIL";
    }
  }

  public IssueVariation getTotal() {
    return total;
  }

  public List<Issue> getIssues() {
    return issues;
  }

  public Map<Integer, List<Issue>> getIssuesPerLine() {
    return issuesPerLine;
  }

  public List<Issue> getIssuesAtLine(int lineId, boolean all) {
    if (all) {
      if (issuesPerLine.containsKey(lineId)) {
        return issuesPerLine.get(lineId);
      }
    } else if (newIssuesPerLine.containsKey(lineId)) {
      return newIssuesPerLine.get(lineId);
    }
    return Collections.emptyList();
  }

  public void addIssue(Issue issue) {
    Severity severity = Severity.create(issue.getSeverity());
    String ruleKey = issue.getRuleKey();
    IssueCategory reportRuleKey = new IssueCategory(ruleKey, severity);

    initMaps(reportRuleKey);
    issues.add(issue);
    Integer line = issue.getStartLine();
    line = line != null ? line : 0;

    getOrCreate(issuesPerLine, line, issueListCreator).add(issue);
    getOrCreate(issuesByRule, ruleKey, intCreator).inc();
    getOrCreate(issuesBySeverity, severity, intCreator).inc();

    reportByCategory.get(reportRuleKey).getTotal().incrementCountInCurrentAnalysis();
    total.incrementCountInCurrentAnalysis();

    if (issue.isNew()) {
      getOrCreate(newIssuesPerLine, line, issueListCreator).add(issue);
      total.incrementNewIssuesCount();
      reportByCategory.get(reportRuleKey).getTotal().incrementNewIssuesCount();
    }
  }

  public void addResolvedIssue(String ruleKey, Severity severity) {
    IssueCategory reportRuleKey = new IssueCategory(ruleKey, severity);
    initMaps(reportRuleKey);
    total.incrementResolvedIssuesCount();
    reportByCategory.get(reportRuleKey).getTotal().incrementResolvedIssuesCount();
  }

  private void initMaps(IssueCategory reportRuleKey) {
    if (!reportByCategory.containsKey(reportRuleKey)) {
      reportByCategory.put(reportRuleKey, new CategoryReport(reportRuleKey));
    }
  }

  public boolean isDisplayableLine(Integer lineNumber, boolean all) {
    if (lineNumber == null || lineNumber < 1) {
      return false;
    }
    for (int i = lineNumber - 2; i <= lineNumber + 2; i++) {
      if (hasIssues(i, all)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasIssues(Integer lineId, boolean all) {
    if (all) {
      List<Issue> issuesAtLine = issuesPerLine.get(lineId);
      return issuesAtLine != null && !issuesAtLine.isEmpty();
    }
    List<Issue> newIssuesAtLine = newIssuesPerLine.get(lineId);
    return newIssuesAtLine != null && !newIssuesAtLine.isEmpty();
  }

  public List<CategoryReport> getCategoryReports() {
    List<CategoryReport> result = new ArrayList<>(reportByCategory.values());
    Collections.sort(result, new CategoryReportComparator());
    return result;
  }

  // waiting for Java 8..
  private static Function<MutableInt> intCreator = new Function<MutableInt>() {
    @Override
    public MutableInt call() {
      return new MutableInt();
    }
  };

  private static Function<List<Issue>> issueListCreator = new Function<List<Issue>>() {
    @Override
    public List<Issue> call() {
      return new LinkedList<>();
    }
  };

}
