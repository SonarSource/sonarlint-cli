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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportFactoryTest {
  private ReportFactory factory;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    factory = new ReportFactory(Charset.defaultCharset());
  }

  @Test
  public void test() {
    List<Reporter> reporters = factory.createReporters(Paths.get("test"));
    assertThat(reporters).hasSize(2);
  }

  @Test
  public void defaultReportFile() {
    Path report = factory.getReportFile(temp.getRoot().toPath());
    assertThat(report).isEqualTo(temp.getRoot().toPath().resolve(".sonarlint").resolve("sonarlint-report.html"));
  }

  @Test
  public void customReportFile() {
    factory.setHtmlPath(Paths.get("myreport", "myfile.html").toString());
    Path report = factory.getReportFile(temp.getRoot().toPath());
    assertThat(report).isEqualTo(temp.getRoot().toPath().resolve("myreport").resolve("myfile.html"));
  }
}
