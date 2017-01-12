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

public class IssueCategoryTest {
  private final static String RULE_KEY = "key";
  private final static String RULE_NAME = "key name";
  private final static Severity SEVERITY = Severity.MAJOR;
  private IssueCategory category;

  @Before
  public void setUp() {
    category = new IssueCategory(RULE_KEY, SEVERITY, RULE_NAME);
  }

  @Test
  public void getters() {
    assertThat(category.getRuleKey()).isEqualTo(RULE_KEY);
    assertThat(category.getSeverity()).isEqualTo(SEVERITY);
    assertThat(category.getName()).isEqualTo(RULE_NAME);
  }
}
