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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonarlint.cli.report.source.HtmlSourceDecorator;
import org.sonarlint.cli.util.Util;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

public class IssuesReport {

  public static final int TOO_MANY_ISSUES_THRESHOLD = 1000;
  private String title;
  private Date date;
  private int filesAnalyzed;
  private final ReportSummary summary = new ReportSummary();
  private final Map<Path, ResourceReport> resourceReportsByFilePath = new HashMap<>();
  private final Map<String, String> ruleNameByKey = new HashMap<>();
  private final Charset charset;
  private int id = 0;
  private Path basePath;

  IssuesReport(Path basePath, Charset charset) {
    this.basePath = basePath;
    this.charset = charset;
  }

  public boolean noIssues() {
    return summary.getTotal().getCountInCurrentAnalysis() == 0;
  }

  public ReportSummary getSummary() {
    return summary;
  }

  public boolean noFiles() {
    return filesAnalyzed == 0;
  }

  public int getFilesAnalyzed() {
    return filesAnalyzed;
  }

  public void setFilesAnalyzed(int num) {
    this.filesAnalyzed = num;
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

  public Map<Path, ResourceReport> getResourceReportsByResource() {
    return resourceReportsByFilePath;
  }

  public List<ResourceReport> getResourceReports() {
    return new ArrayList<>(resourceReportsByFilePath.values());
  }

  public List<Path> getResourcesWithReport() {
    return new ArrayList<>(resourceReportsByFilePath.keySet());
  }

  public String getRuleName(String ruleKey) {
    return ruleNameByKey.get(ruleKey);
  }

  public void addIssue(Issue issue) {
    IssueWithId issueWithId = new IssueWithIdImpl(issue, id);
    id++;
    ruleNameByKey.put(issue.getRuleKey(), issue.getRuleName());

    Path filePath;
    ClientInputFile inputFile = issue.getInputFile();
    if (inputFile == null) {
      // issue on project (no specific file)
      filePath = Paths.get("");
    } else {
      filePath = inputFile.getPath();
    }
    ResourceReport report = getOrCreate(filePath);
    getSummary().addIssue(issueWithId);
    report.addIssue(issueWithId);
  }

  private static class IssueWithIdImpl implements IssueWithId {

    private final Issue wrapped;
    private final int id;

    public IssueWithIdImpl(Issue wrapped, int id) {
      this.wrapped = wrapped;
      this.id = id;
    }

    @Override
    public String getSeverity() {
      return wrapped.getSeverity();
    }

    @Override
    public Integer getStartLine() {
      return wrapped.getStartLine() != null ? wrapped.getStartLine() : 1;
    }

    @Override
    public Integer getStartLineOffset() {
      return wrapped.getStartLineOffset();
    }

    @Override
    public Integer getEndLine() {
      return wrapped.getEndLine() != null ? wrapped.getEndLine() : getStartLine();

    }

    @Override
    public Integer getEndLineOffset() {
      return wrapped.getEndLineOffset();

    }

    @Override
    public String getMessage() {
      return wrapped.getMessage();

    }

    @Override
    public String getRuleKey() {
      return wrapped.getRuleKey();

    }

    @Override
    public String getRuleName() {
      return wrapped.getRuleName();

    }

    @Override
    public ClientInputFile getInputFile() {
      return wrapped.getInputFile();

    }

    @Override
    public int id() {
      return id;
    }

    @Override
    public String ruleDescriptionFileName() {
      return Util.escapeFileName(wrapped.getRuleKey()) + ".html";
    }

  }

  private ResourceReport getOrCreate(Path filePath) {
    ResourceReport report = resourceReportsByFilePath.get(filePath);
    if (report != null) {
      return report;
    }
    report = new ResourceReport(basePath, filePath);
    resourceReportsByFilePath.put(filePath, report);
    return report;
  }

  public List<String> getEscapedSource(Path filePath) {
    if (filePath == null) {
      return Collections.emptyList();
    }
    List<String> lines;
    try {
      if (!Files.isRegularFile(filePath)) {
        // invalid, directory, project issue, ...
        return Collections.emptyList();
      }

      lines = Files.readAllLines(filePath, charset);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read source code of file: " + filePath, e);
    }
    ResourceReport resourceReport = resourceReportsByFilePath.get(filePath);
    List<String> escapedLines = new ArrayList<>(lines.size());
    int lineIdx = 1;
    for (String line : lines) {
      final int currentLineIdx = lineIdx;
      List<IssueWithId> issuesAtLine = resourceReport != null
        ? resourceReport.getIssues().stream()
          .filter(i -> i.getStartLine() <= currentLineIdx && i.getEndLine() >= currentLineIdx)
          .collect(Collectors.toList())
        : Collections.emptyList();

      escapedLines.add(HtmlSourceDecorator.getDecoratedSourceAsHtml(line, currentLineIdx, issuesAtLine));
      lineIdx++;
    }
    return escapedLines;

  }

}
