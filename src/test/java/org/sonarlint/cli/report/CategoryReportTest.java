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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryReportTest {
  private CategoryReport report;
  private IssueCategory cat;

  @Before
  public void setUp() {
    cat = mock(IssueCategory.class);
    when(cat.getRuleKey()).thenReturn("rule1");
    when(cat.getSeverity()).thenReturn(Severity.MINOR);
    report = new CategoryReport(cat);
  }

  @Test
  public void testGetters() {
    assertThat(report.getRuleKey()).isEqualTo("rule1");
    assertThat(report.getSeverity()).isEqualTo(Severity.MINOR);
    assertThat(report.getCategory()).isEqualTo(cat);

  }
}
