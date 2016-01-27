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

import org.sonarlint.cli.util.Logger;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class ReportFactory {
  private static final String DEFAULT_REPORT_NAME = "issues-report";
  private Logger logger = Logger.get();

  private String reportName = null;
  private String reportDir = null;

  public List<Reporter> createReporters(String workDir) {
    List<Reporter> list = new LinkedList<>();
    Path basePath = Paths.get(workDir);

    list.add(new ConsoleReport());
    list.add(new HtmlReport(getReportFile(workDir), new SourceProvider(basePath, StandardCharsets.UTF_8)));

    return list;
  }

  public void setDir(@Nullable String dir) {
    reportDir = dir;
  }

  public void setName(@Nullable String name) {
    reportName = name;
  }

  private Path getReportFile(String workDir) {
    if (reportDir == null) {
      reportDir = workDir;
    }
    if (reportName == null) {
      reportName = DEFAULT_REPORT_NAME;
    }

    Path reportFileDir = Paths.get(reportDir);
    if (!reportFileDir.isAbsolute()) {
      reportFileDir = Paths.get(workDir, reportDir);
    }
    if (reportDir.endsWith(".html")) {
      logger.warn("'" + reportDir + "' should indicate a directory. Using parent folder.");
      reportFileDir = reportFileDir.getParent();
    }
    try {
      Files.createDirectories(reportFileDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory " + reportDir, e);
    }
    return reportFileDir.resolve(reportName);
  }
}
