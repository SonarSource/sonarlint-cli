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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesReport {

  public static final int TOO_MANY_ISSUES_THRESHOLD = 1000;
  private String title;
  private Date date;
  private final ReportSummary summary = new ReportSummary();
  private final Map<String, ResourceReport> resourceReportsByResource = new HashMap<>();

  IssuesReport() {

  }

  public boolean noIssues() {
    return summary.getTotal().getCountInCurrentAnalysis() == 0;
  }

  public ReportSummary getSummary() {
    return summary;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Map<String, ResourceReport> getResourceReportsByResource() {
    return resourceReportsByResource;
  }

  public List<ResourceReport> getResourceReports() {
    return new ArrayList<>(resourceReportsByResource.values());
  }

  public List<String> getResourcesWithReport() {
    return new ArrayList<>(resourceReportsByResource.keySet());
  }

  public void addIssue(Issue issue) {
    Severity severity = Severity.create(issue.getSeverity());
    ResourceReport report = getOrCreate(issue.getComponentKey());
    getSummary().addIssue(issue);

    if (issue.getResolution() != null) {
      report.addResolvedIssue(issue.getRuleKey(), severity);
    } else {
      report.addIssue(issue);
    }
  }

  private ResourceReport getOrCreate(String resource) {
    ResourceReport report = resourceReportsByResource.get(resource);
    if (report != null) {
      return report;
    }
    report = new ResourceReport(resource);
    resourceReportsByResource.put(resource, new ResourceReport(resource));
    return report;
  }
}
