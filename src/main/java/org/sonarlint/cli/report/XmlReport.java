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

import freemarker.template.Template;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.AnalysisResults;
import org.sonarsource.sonarlint.core.IssueListener;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlReport implements Reporter {
  private static final Logger LOGGER = Logger.get();
  private final Path reportFile;
  private final Path reportDir;
  private final SourceProvider sourceProvider;
  private Path basePath;

  public XmlReport(Path basePath, Path reportFile, SourceProvider sourceProvider) {
    this.basePath = basePath;
    this.sourceProvider = sourceProvider;
    this.reportDir = reportFile.getParent().toAbsolutePath();
    this.reportFile = reportFile.toAbsolutePath();
  }

  @Override
  public void execute(String projectName, Date date, List<IssueListener.Issue> issues, AnalysisResults result) {
    IssuesReport report = new IssuesReport(basePath);
    for (IssueListener.Issue i : issues) {
      report.addIssue(i);
    }
    report.setTitle(projectName);
    report.setDate(date);
    report.setFilesAnalyzed(result.fileCount());
    print(report);
  }

  public void print(IssuesReport report) {
    LOGGER.debug("Generating SonarLint XML Report to: " + reportFile);
    writeToFile(report, reportFile);
    LOGGER.info("SonarLint XML Report generated: " + reportFile);
  }

  private void writeToFile(IssuesReport report, Path toFile) {
    try {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(XmlReport.class, "");

      Map<String, Object> root = new HashMap<>();
      root.put("report", report);
      root.put("sources", sourceProvider);

      Template template = cfg.getTemplate("sonarlintxmlreport.ftl");

      try (FileOutputStream fos = new FileOutputStream(toFile.toFile());
           Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
        template.process(root, writer);
        writer.flush();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate XML Issues Report to: " + toFile, e);
    }
  }
}
