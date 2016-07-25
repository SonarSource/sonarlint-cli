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

import java.nio.file.Path;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlReportTest {
  private HtmlReport html;
  private AnalysisResults result;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private Path reportFile;
  private SourceProvider sources;

  @Before
  public void setUp() {
    result = mock(AnalysisResults.class);
    when(result.fileCount()).thenReturn(1);
    reportFile = temp.getRoot().toPath().resolve("report.html");
    sources = mock(SourceProvider.class);
    html = new HtmlReport(temp.getRoot().toPath(), reportFile, sources);
  }

  @Test
  public void testHtml() {
    html.execute("project", new Date(), createTestIssues(), result);
  }

  private static List<Issue> createTestIssues() {
    return new LinkedList<>();
  }
}
