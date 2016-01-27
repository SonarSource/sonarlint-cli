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

import freemarker.template.Template;
import org.sonar.runner.api.Issue;
import org.sonarlint.cli.util.Logger;

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

public class HtmlReport implements Reporter {
  private final Logger logger;
  private final Path reportFile;
  private final Path reportDir;
  private final SourceProvider sourceProvider;

  HtmlReport(Path reportFile, SourceProvider sourceProvider) {
    this.sourceProvider = sourceProvider;
    this.reportDir = reportFile.getParent().toAbsolutePath();
    this.reportFile = reportFile.toAbsolutePath();
    this.logger = Logger.get();
  }

  @Override
  public void execute(String projectName, Date date, List<Issue> issues) {
    IssuesReport report = new IssuesReport();
    for (Issue i : issues) {
      report.addIssue(i);
    }
    report.setTitle(projectName);
    report.setDate(date);
    print(report);
  }

  public void print(IssuesReport report) {
    logger.debug("Generating HTML Report to: " + reportFile);
    writeToFile(report, reportFile);
    logger.info("HTML Issues Report generated: " + reportFile);
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

      Template template = cfg.getTemplate("issuesreport.ftl");

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
    Path target = toDir.resolve("issuesreport_files");
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
    copyDependency(target, "sonarqube-24x100.png");
  }

  private void copyDependency(Path target, String filename) {
    String resource = "issuesreport_files/" + filename;
    try (InputStream in = getClass().getResourceAsStream(resource)) {
      Files.copy(in, target.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy file " + filename + " to " + target, e);
    }
  }
}
