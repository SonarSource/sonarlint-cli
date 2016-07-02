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

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class ReportFactory {
  private static final String DEFAULT_REPORT_PATH = ".sonarlint/sonarlint-report.html";
  private String htmlPath = null;
  private String xmlPath = null;
  private Charset charset;

  public ReportFactory(Charset charset) {
    this.charset = charset;
  }

  public List<Reporter> createReporters(Path basePath) {
    List<Reporter> list = new LinkedList<>();

    list.add(new ConsoleReport());
    list.add(new HtmlReport(basePath, getReportFile(basePath, htmlPath), new SourceProvider(charset)));
    if (xmlPath != null) {
      list.add(new XmlReport(basePath, getReportFile(basePath, xmlPath), new SourceProvider(charset)));
    }

    return list;
  }

  public void setHtmlPath(@Nullable String path) {
    htmlPath = path;
  }

  public void setXmlPath(@Nullable String path) {
    xmlPath = path;
  }

  Path getReportFile(Path basePath) {
    return getReportFile(basePath, null);
  }

  Path getReportFile(Path basePath, String filePath) {
    Path reportPath;

    if (filePath != null) {
      reportPath = Paths.get(filePath);

      if (!reportPath.isAbsolute()) {
        reportPath = basePath.resolve(reportPath).toAbsolutePath();
      }
    } else {
      reportPath = basePath.resolve(DEFAULT_REPORT_PATH);
    }

    try {
      Files.createDirectories(reportPath.getParent());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory " + reportPath.getParent(), e);
    }

    return reportPath;
  }
}
