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

import java.util.Date;
import java.util.List;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.IssueListener;

public class ConsoleReport implements Reporter {

  public static final String HEADER = "-------------  Issues Report  -------------";

  private final Logger logger = Logger.get();

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

    public void process(IssueListener.Issue issue) {
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
          throw new IllegalStateException("Unknow severity: " + issue.getSeverity());
      }
    }

    public boolean hasNoIssues() {
      return totalIssues == 0;
    }
  }

  @Override
  public void execute(String projectName, Date date, List<IssueListener.Issue> issues) {
    Report r = new Report();
    for (IssueListener.Issue issue : issues) {
      r.process(issue);
    }
    printReport(r);
  }

  public void printReport(Report r) {
    StringBuilder sb = new StringBuilder();

    sb.append("\n\n" + HEADER + "\n\n");
    if (r.hasNoIssues()) {
      sb.append("  No issues to display\n");
    } else {
      printIssues(r, sb);
    }
    sb.append("\n-------------------------------------------\n\n");

    logger.info(sb.toString());
  }

  private void printIssues(Report r, StringBuilder sb) {
    int issues = r.totalIssues;
    if (issues > 0) {
      sb.append(leftPad(Integer.toString(issues), LEFT_PAD)).append(" issue" + (issues > 1 ? "s" : "")).append("\n\n");
      printIssues(sb, r.blockerIssues, "blocker");
      printIssues(sb, r.criticalIssues, "critical");
      printIssues(sb, r.majorIssues, "major");
      printIssues(sb, r.minorIssues, "minor");
      printIssues(sb, r.infoIssues, "info");
    } else {
      sb.append("  No issue").append("\n");
    }
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
