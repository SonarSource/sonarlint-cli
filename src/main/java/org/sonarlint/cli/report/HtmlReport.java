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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

public class HtmlReport implements Reporter {
  private static final Logger LOGGER = Logger.get();
  private final Path reportFile;
  private final Path reportDir;
  private final SourceProvider sourceProvider;
  private Path basePath;

  HtmlReport(Path basePath, Path reportFile, SourceProvider sourceProvider) {
    this.basePath = basePath;
    this.sourceProvider = sourceProvider;
    this.reportDir = reportFile.getParent().toAbsolutePath();
    this.reportFile = reportFile.toAbsolutePath();
  }

  @Override
  public void execute(String projectName, Date date, List<Issue> issues, AnalysisResults result) {
    IssuesReport report = new IssuesReport(basePath);
    for (Issue i : issues) {
      report.addIssue(i);
    }
    report.setTitle(projectName);
    report.setDate(date);
    report.setFilesAnalyzed(result.fileCount());
    print(report);
  }

  public void print(IssuesReport report) {
    LOGGER.debug("Generating SonarLint Report to: " + reportFile);
    writeToFile(report, reportFile);
    LOGGER.info("SonarLint HTML Report generated: " + reportFile);
    try {
      copyDependencies(reportDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to copy HTML report resources to: " + reportDir, e);
    }
  }

  private void writeToFile(IssuesReport report, Path toFile) {
    try {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(HtmlReport.class, "");

      Map<String, Object> root = new HashMap<>();
      root.put("report", report);
      root.put("sources", sourceProvider);

      Template template = cfg.getTemplate("sonarlintreport.ftl");

      try (FileOutputStream fos = new FileOutputStream(toFile.toFile());
        Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
        template.process(root, writer);
        writer.flush();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML Issues Report to: " + toFile, e);
    }
  }

  private void copyDependencies(Path toDir) throws URISyntaxException, IOException {
    Path target = toDir.resolve("sonarlintreport_files");
    Files.createDirectories(target);

    // I don't know how to extract a directory from classpath, that's why an exhaustive list of files is provided here :
    copyDependency(target, "sonar.eot");
    copyDependency(target, "sonar.svg");
    copyDependency(target, "sonar.ttf");
    copyDependency(target, "sonar.woff");
    copyDependency(target, "favicon.ico");
    copyDependency(target, "PRJ.png");
    copyDependency(target, "DIR.png");
    copyDependency(target, "FIL.png");
    copyDependency(target, "jquery.min.js");
    copyDependency(target, "sep12.png");
    copyDependency(target, "sonar.css");
    copyDependency(target, "sonarlint.png");
  }

  private void copyDependency(Path target, String filename) {
    String resource = "sonarlintreport_files/" + filename;
    try (InputStream in = getClass().getResourceAsStream(resource)) {
      Files.copy(in, target.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy file " + filename + " to " + target, e);
    }
  }
}
