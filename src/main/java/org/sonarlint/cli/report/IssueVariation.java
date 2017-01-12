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

public class IssueVariation {

  private int countInCurrentAnalysis;
  private int newIssuesCount;
  private int resolvedIssuesCount;

  IssueVariation() {
  }

  IssueVariation(int currentCount, int newCount, int resolvedCount) {
    this.countInCurrentAnalysis = currentCount;
    this.newIssuesCount = newCount;
    this.resolvedIssuesCount = resolvedCount;
  }

  public int getCountInCurrentAnalysis() {
    return countInCurrentAnalysis;
  }

  public void incrementCountInCurrentAnalysis() {
    this.countInCurrentAnalysis++;
  }

  public int getNewIssuesCount() {
    return newIssuesCount;
  }

  public void incrementNewIssuesCount() {
    this.newIssuesCount++;
  }

  public int getResolvedIssuesCount() {
    return resolvedIssuesCount;
  }

  public void incrementResolvedIssuesCount() {
    this.resolvedIssuesCount++;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + countInCurrentAnalysis;
    result = prime * result + newIssuesCount;
    result = prime * result + resolvedIssuesCount;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    IssueVariation other = (IssueVariation) obj;

    return countInCurrentAnalysis == other.countInCurrentAnalysis
      && newIssuesCount == other.newIssuesCount
      && resolvedIssuesCount == other.resolvedIssuesCount;
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append(super.toString())
      .append("[countInCurrentAnalysis=")
      .append(countInCurrentAnalysis)
      .append(",newIssuesCount=")
      .append(newIssuesCount)
      .append(",resolvedIssuesCount=")
      .append(resolvedIssuesCount)
      .append("]")
      .toString();
  }

}
