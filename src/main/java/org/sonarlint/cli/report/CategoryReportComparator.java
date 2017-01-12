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

import java.util.Comparator;

class CategoryReportComparator implements Comparator<CategoryReport> {
  @Override
  public int compare(CategoryReport o1, CategoryReport o2) {
    if (bothHaveNoNewIssue(o1, o2)) {
      return compareByRuleSeverityAndKey(o1, o2);
    } else if (bothHaveNewIssues(o1, o2)) {
      if (sameSeverity(o1, o2) && !sameNewIssueCount(o1, o2)) {
        return compareNewIssueCount(o1, o2);
      } else {
        return compareByRuleSeverityAndKey(o1, o2);
      }
    } else {
      return compareNewIssueCount(o1, o2);
    }
  }

  private static int compareByRuleSeverityAndKey(CategoryReport o1, CategoryReport o2) {
    return o1.getCategory().compareTo(o2.getCategory());
  }

  private static boolean sameNewIssueCount(CategoryReport o1, CategoryReport o2) {
    return o2.getTotal().getNewIssuesCount() == o1.getTotal().getNewIssuesCount();
  }

  private static boolean sameSeverity(CategoryReport o1, CategoryReport o2) {
    return o1.getSeverity().equals(o2.getSeverity());
  }

  private static int compareNewIssueCount(CategoryReport o1, CategoryReport o2) {
    return o2.getTotal().getNewIssuesCount() - o1.getTotal().getNewIssuesCount();
  }

  private static boolean bothHaveNewIssues(CategoryReport o1, CategoryReport o2) {
    return o1.getTotal().getNewIssuesCount() > 0 && o2.getTotal().getNewIssuesCount() > 0;
  }

  private static boolean bothHaveNoNewIssue(CategoryReport o1, CategoryReport o2) {
    return o1.getTotal().getNewIssuesCount() == 0 && o2.getTotal().getNewIssuesCount() == 0;
  }
}
