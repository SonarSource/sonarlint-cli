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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonarlint.cli.TestUtils.createTestIssue;

public class ResourceReportTest {
  private final static Path RESOURCE = Paths.get("resource");
  private ResourceReport resourceReport;

  @Before
  public void setUp() {
    resourceReport = new ResourceReport(Paths.get(""), RESOURCE);
  }

  @Test
  public void testIssuesLines() {
    RichIssue i1 = createTestIssue("file1", "rule1", "MAJOR", 10);
    RichIssue i2 = createTestIssue("file1", "rule1", "MAJOR", 11);
    resourceReport.addIssue(i1);
    resourceReport.addIssue(i2);

    assertThat(resourceReport.getIssues()).containsOnly(i1, i2);
    assertThat(resourceReport.getIssuesAtLine(10)).containsExactly(i1);
    assertThat(resourceReport.getIssuesAtLine(20)).isEmpty();
    assertThat(resourceReport.getIssuesPerLine()).containsOnly(
      entry(i1.getStartLine(), Collections.singletonList(i1)),
      entry(i2.getStartLine(), Collections.singletonList(i2)));
    assertThat(resourceReport.getName()).isEqualTo("resource");
    assertThat(resourceReport.getPath()).isEqualTo(RESOURCE);
  }

  @Test
  public void testType() {
    assertThat(resourceReport.getType()).isEqualTo("FIL");

    resourceReport = new ResourceReport(Paths.get(""), Paths.get(""));
    assertThat(resourceReport.getType()).isEqualTo("PRJ");
  }

  @Test
  public void testName() {
    resourceReport = new ResourceReport(Paths.get("/tmp/test"), Paths.get("/tmp/test/src/file1"));
    assertThat(resourceReport.getName()).isEqualTo("src" + File.separator + "file1");
  }

  @Test
  public void testCategoryReport() {
    RichIssue i1 = createTestIssue("file1", "rule1", "MAJOR", 10);
    RichIssue i2 = createTestIssue("file1", "rule1", "MINOR", 11);
    RichIssue i3 = createTestIssue("file1", "rule2", "MINOR", 11);
    RichIssue i4 = createTestIssue("file1", "rule2", "MINOR", 12);
    resourceReport.addIssue(i1);
    resourceReport.addIssue(i2);
    resourceReport.addIssue(i3);
    resourceReport.addIssue(i4);

    List<Severity> l = new LinkedList<>();
    l.add(Severity.MINOR);
    l.add(Severity.MAJOR);
    Collections.sort(l);

    List<CategoryReport> categoryReports = resourceReport.getCategoryReports();
    assertThat(categoryReports).hasSize(3);

    // sort first by severity, then by rule key
    assertThat(categoryReports).extracting("ruleKey").containsExactly("rule1", "rule1", "rule2");
    assertThat(categoryReports).extracting("severity").containsExactly(Severity.MAJOR, Severity.MINOR, Severity.MINOR);

    // grouping
    assertThat(categoryReports.get(0).getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(categoryReports.get(1).getTotal().getCountInCurrentAnalysis()).isEqualTo(1);
    assertThat(categoryReports.get(2).getTotal().getCountInCurrentAnalysis()).isEqualTo(2);

    assertThat(resourceReport.getTotal().getCountInCurrentAnalysis()).isEqualTo(4);
  }

  @Test
  public void lineIssues() {
    RichIssue i1 = createTestIssue("file1", "rule1", "MAJOR", 10);
    RichIssue i2 = createTestIssue("file1", "rule1", "MINOR", 11);
    resourceReport.addIssue(i1);
    resourceReport.addIssue(i2);

    assertThat(resourceReport.isDisplayableLine(0)).isFalse();
    assertThat(resourceReport.isDisplayableLine(-3)).isFalse();
    assertThat(resourceReport.isDisplayableLine(null)).isFalse();

    assertThat(resourceReport.isDisplayableLine(7)).isFalse();
    assertThat(resourceReport.isDisplayableLine(8)).isTrue();
    assertThat(resourceReport.isDisplayableLine(9)).isTrue();
    assertThat(resourceReport.isDisplayableLine(10)).isTrue();
    assertThat(resourceReport.isDisplayableLine(11)).isTrue();
    assertThat(resourceReport.isDisplayableLine(12)).isTrue();
    assertThat(resourceReport.isDisplayableLine(13)).isTrue();
    assertThat(resourceReport.isDisplayableLine(14)).isFalse();

  }
}
