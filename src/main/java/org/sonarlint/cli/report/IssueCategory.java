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

import java.util.Objects;

/**
 * A same rule can be present with different severity if severity was manually changed so we categorize by rule key and severity
 *
 */
public class IssueCategory implements Comparable<IssueCategory> {
  private final String ruleKey;
  private final Severity severity;
  private final String name;

  IssueCategory(String ruleKey, Severity severity, String name) {
    this.ruleKey = ruleKey;
    this.severity = severity;
    this.name = name;
  }

  public String getRuleKey() {
    return ruleKey;
  }
  
  public String getName() {
    return name;
  }

  public Severity getSeverity() {
    return severity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueCategory that = (IssueCategory) o;
    return Objects.equals(ruleKey, that.ruleKey) && Objects.equals(severity, that.severity);
  }

  @Override
  public int hashCode() {
    int result = ruleKey.hashCode();
    result = 31 * result + severity.hashCode();
    return result;
  }

  @Override
  public int compareTo(IssueCategory o) {
    if (severity == o.getSeverity()) {
      return getRuleKey().compareTo(o.getRuleKey());
    }
    return o.getSeverity().compareTo(severity);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    return sb.append(super.toString())
      .append("[")
      .append("rule=").append(ruleKey).append(",severity=").append(severity)
      .append("]").toString();
  }
}
