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
package org.sonarlint.cli.report.source;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import org.sonarlint.cli.report.RichIssue;

class DecorationDataHolder {

  private List<OpeningHtmlTag> openingTagsEntries;
  private int openingTagsIndex;
  private List<Integer> closingTagsOffsets;
  private int closingTagsIndex;

  DecorationDataHolder() {
    openingTagsEntries = Lists.newArrayList();
    closingTagsOffsets = Lists.newArrayList();
  }

  void loadIssues(List<RichIssue> issues, int currentLineIdx, int currentLineLength) {
    for (RichIssue issue : issues) {
      int startOffset = issue.getStartLine() == currentLineIdx && issue.getStartLineOffset() != null ? issue.getStartLineOffset() : 0;
      int endOffset = issue.getEndLine() == currentLineIdx && issue.getEndLineOffset() != null ? issue.getEndLineOffset() : currentLineLength;
      if (startOffset < endOffset) {
        insertAndPreserveOrder(new OpeningHtmlTag(startOffset, "issue-" + issue.id()), openingTagsEntries);
        insertAndPreserveOrder(endOffset, closingTagsOffsets);
      }
    }
  }

  OpeningHtmlTag getCurrentOpeningTagEntry() {
    return openingTagsIndex < openingTagsEntries.size() ? openingTagsEntries.get(openingTagsIndex) : null;
  }

  void nextOpeningTagEntry() {
    openingTagsIndex++;
  }

  int getCurrentClosingTagOffset() {
    return closingTagsIndex < closingTagsOffsets.size() ? closingTagsOffsets.get(closingTagsIndex) : -1;
  }

  void nextClosingTagOffset() {
    closingTagsIndex++;
  }

  private static void insertAndPreserveOrder(OpeningHtmlTag newEntry, List<OpeningHtmlTag> openingHtmlTags) {
    int insertionIndex = 0;
    Iterator<OpeningHtmlTag> tagIterator = openingHtmlTags.iterator();
    while (tagIterator.hasNext() && tagIterator.next().getStartOffset() <= newEntry.getStartOffset()) {
      insertionIndex++;
    }
    openingHtmlTags.add(insertionIndex, newEntry);
  }

  private static void insertAndPreserveOrder(int newOffset, List<Integer> orderedOffsets) {
    int insertionIndex = 0;
    Iterator<Integer> entriesIterator = orderedOffsets.iterator();
    while (entriesIterator.hasNext() && entriesIterator.next() <= newOffset) {
      insertionIndex++;
    }
    orderedOffsets.add(insertionIndex, newOffset);
  }
}
