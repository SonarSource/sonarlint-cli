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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarlint.cli.util.MutableInt;

import static org.sonarlint.cli.util.Util.getOrCreate;

public final class ResourceReport {
  private final Path filePath;
  private final IssueVariation total = new IssueVariation();
  private final List<RichIssue> issues = new ArrayList<>();

  private final Map<IssueCategory, CategoryReport> reportByCategory = new HashMap<>();
  private final Map<Integer, List<RichIssue>> issuesPerLine = new HashMap<>();

  private final Map<String, MutableInt> issuesByRule = new HashMap<>();
  private final EnumMap<Severity, MutableInt> issuesBySeverity = new EnumMap<>(Severity.class);
  private Path basePath;

  ResourceReport(Path basePath, @Nullable Path filePath) {
    this.basePath = basePath;
    this.filePath = filePath;
  }

  @CheckForNull
  public Path getPath() {
    return filePath;
  }

  public String getName() {
    return basePath.toAbsolutePath().relativize(filePath.toAbsolutePath()).toString();
  }

  public String getType() {
    if (filePath == null || filePath.equals(Paths.get(""))) {
      return "PRJ";
    } else {
      return "FIL";
    }
  }

  public IssueVariation getTotal() {
    return total;
  }

  public List<RichIssue> getIssues() {
    return issues;
  }

  public Map<Integer, List<RichIssue>> getIssuesPerLine() {
    return issuesPerLine;
  }

  public List<RichIssue> getIssuesAtLine(int lineId) {
    if (issuesPerLine.containsKey(lineId)) {
      return issuesPerLine.get(lineId);
    }
    return Collections.emptyList();
  }

  public void addIssue(RichIssue issue) {
    Severity severity = Severity.create(issue.getSeverity());
    String ruleKey = issue.getRuleKey();
    IssueCategory reportRuleKey = new IssueCategory(ruleKey, severity, issue.getRuleName());

    initMaps(reportRuleKey);
    issues.add(issue);
    Integer line = issue.getStartLine();
    line = line != null ? line : 0;

    getOrCreate(issuesPerLine, line, LinkedList::new).add(issue);
    getOrCreate(issuesByRule, ruleKey, MutableInt::new).inc();
    getOrCreate(issuesBySeverity, severity, MutableInt::new).inc();

    reportByCategory.get(reportRuleKey).getTotal().incrementCountInCurrentAnalysis();
    total.incrementCountInCurrentAnalysis();
  }

  private void initMaps(IssueCategory reportRuleKey) {
    if (!reportByCategory.containsKey(reportRuleKey)) {
      reportByCategory.put(reportRuleKey, new CategoryReport(reportRuleKey));
    }
  }

  public boolean isDisplayableLine(@Nullable Integer lineNumber) {
    if (lineNumber == null || lineNumber < 1) {
      return false;
    }
    for (int i = lineNumber - 2; i <= lineNumber + 2; i++) {
      if (hasIssues(i)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasIssues(Integer lineId) {
    List<RichIssue> issuesAtLine = issuesPerLine.get(lineId);
    return issuesAtLine != null && !issuesAtLine.isEmpty();
  }

  public List<CategoryReport> getCategoryReports() {
    List<CategoryReport> result = new ArrayList<>(reportByCategory.values());
    Collections.sort(result, new CategoryReportComparator());
    return result;
  }
}
