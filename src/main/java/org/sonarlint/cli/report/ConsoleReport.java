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

import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.tracking.Trackable;

public class ConsoleReport implements Reporter {

  public static final String HEADER = "-------------  SonarLint Report  -------------";
  private static final Logger LOGGER = Logger.get();

  public static final String CONSOLE_REPORT_ENABLED_KEY = "sonar.issuesReport.console.enable";
  private static final int LEFT_PAD = 10;

  ConsoleReport() {

  }

  private static class Report {
    int totalIssues = 0;
    int blockerIssues = 0;
    int criticalIssues = 0;
    int majorIssues = 0;
    int minorIssues = 0;
    int infoIssues = 0;

    public void process(Issue issue) {
      totalIssues++;
      switch (issue.getSeverity()) {
        case "BLOCKER":
          blockerIssues++;
          break;
        case "CRITICAL":
          criticalIssues++;
          break;
        case "MAJOR":
          majorIssues++;
          break;
        case "MINOR":
          minorIssues++;
          break;
        case "INFO":
          infoIssues++;
          break;
        default:
          throw new IllegalStateException("Unknown severity: " + issue.getSeverity());
      }
    }

    public boolean hasNoIssues() {
      return totalIssues == 0;
    }
  }

  @Override
  public void execute(String projectName, Date date, Collection<Trackable> trackables, AnalysisResults result, Function<String, RuleDetails> ruleDescriptionProducer) {
    Report r = new Report();
    for (Trackable trackable : trackables) {
      r.process(trackable.getIssue());
    }
    printReport(r, result);
  }

  public void printReport(Report r, AnalysisResults result) {
    StringBuilder sb = new StringBuilder();

    sb.append("\n\n" + HEADER + "\n\n");
    if (result.fileCount() == 0) {
      sb.append("  No files analyzed\n");
    } else if (r.hasNoIssues()) {
      sb.append("  No issues to display ");
      filesAnalyzed(sb, result.fileCount());
      sb.append("\n");
    } else {
      printIssues(r, sb, result.fileCount());
    }
    sb.append("\n-------------------------------------------\n\n");

    LOGGER.info(sb.toString());
  }

  private static void filesAnalyzed(StringBuilder sb, int num) {
    sb.append("(").append(num);
    if (num > 1) {
      sb.append(" files analyzed");
    } else {
      sb.append(" file analyzed");
    }
    sb.append(")");
  }

  private static void printIssues(Report r, StringBuilder sb, int filesAnalyzed) {
    int issues = r.totalIssues;
    sb.append(leftPad(Integer.toString(issues), LEFT_PAD))
      .append(" issue");
    if (issues > 1) {
      sb.append("s");
    }
    sb.append(" ");

    filesAnalyzed(sb, filesAnalyzed);
    sb.append("\n\n");
    printIssues(sb, r.blockerIssues, "blocker");
    printIssues(sb, r.criticalIssues, "critical");
    printIssues(sb, r.majorIssues, "major");
    printIssues(sb, r.minorIssues, "minor");
    printIssues(sb, r.infoIssues, "info");
  }

  private static void printIssues(StringBuilder sb, int issueCount, String severityLabel) {
    if (issueCount > 0) {
      sb.append(leftPad(Integer.toString(issueCount), LEFT_PAD)).append(" ").append(severityLabel).append("\n");
    }
  }

  private static String leftPad(String str, int spaces) {
    StringBuilder sb = new StringBuilder();

    for (int i = spaces; i > 0; i--) {
      sb.append(" ");
    }

    sb.append(str);
    return sb.toString();
  }
}
