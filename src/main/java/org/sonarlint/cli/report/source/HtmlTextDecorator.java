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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;

class HtmlTextDecorator {

  static final char HTML_OPENING = '<';
  static final char HTML_CLOSING = '>';
  static final char AMPERSAND = '&';
  static final String ENCODED_HTML_OPENING = "&lt;";
  static final String ENCODED_HTML_CLOSING = "&gt;";
  static final String ENCODED_AMPERSAND = "&amp;";

  String decorateLineWithHtml(String line, DecorationDataHolder decorationDataHolder) {

    StringBuilder currentHtmlLine = new StringBuilder();

    BufferedReader stringBuffer = new BufferedReader(new StringReader(line));

    CharactersReader charsReader = new CharactersReader(stringBuffer);

    try {
      while (charsReader.readNextChar()) {
        addCharToCurrentLine(charsReader, currentHtmlLine, decorationDataHolder);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error when decorating source", e);
    }

    closeCurrentSyntaxTags(charsReader, currentHtmlLine);

    return currentHtmlLine.toString();

  }

  private static void addCharToCurrentLine(CharactersReader charsReader, StringBuilder currentHtmlLine, DecorationDataHolder decorationDataHolder) {
    int numberOfTagsToClose = getNumberOfTagsToClose(charsReader.getCurrentIndex(), decorationDataHolder);
    closeCompletedTags(charsReader, numberOfTagsToClose, currentHtmlLine);

    Collection<String> tagsToOpen = getTagsToOpen(charsReader.getCurrentIndex(), decorationDataHolder);
    openNewTags(charsReader, tagsToOpen, currentHtmlLine);

    char currentChar = (char) charsReader.getCurrentValue();
    currentHtmlLine.append(normalize(currentChar));
  }

  private static char[] normalize(char currentChar) {
    char[] normalizedChars;
    if (currentChar == HTML_OPENING) {
      normalizedChars = ENCODED_HTML_OPENING.toCharArray();
    } else if (currentChar == HTML_CLOSING) {
      normalizedChars = ENCODED_HTML_CLOSING.toCharArray();
    } else if (currentChar == AMPERSAND) {
      normalizedChars = ENCODED_AMPERSAND.toCharArray();
    } else {
      normalizedChars = new char[] {currentChar};
    }
    return normalizedChars;
  }

  private static int getNumberOfTagsToClose(int currentIndex, DecorationDataHolder dataHolder) {
    int numberOfTagsToClose = 0;

    while (currentIndex == dataHolder.getCurrentClosingTagOffset()) {
      numberOfTagsToClose++;
      dataHolder.nextClosingTagOffset();
    }
    return numberOfTagsToClose;
  }

  private static Collection<String> getTagsToOpen(int currentIndex, DecorationDataHolder dataHolder) {
    Collection<String> tagsToOpen = newArrayList();
    while (dataHolder.getCurrentOpeningTagEntry() != null && currentIndex == dataHolder.getCurrentOpeningTagEntry().getStartOffset()) {
      tagsToOpen.add(dataHolder.getCurrentOpeningTagEntry().getCssClass());
      dataHolder.nextOpeningTagEntry();
    }
    return tagsToOpen;
  }

  private static void closeCompletedTags(CharactersReader charactersReader, int numberOfTagsToClose,
    StringBuilder decoratedText) {
    for (int i = 0; i < numberOfTagsToClose; i++) {
      injectClosingHtml(decoratedText);
      charactersReader.removeLastOpenTag();
    }
  }

  private static void openNewTags(CharactersReader charactersReader, Collection<String> tagsToOpen,
    StringBuilder decoratedText) {
    for (String tagToOpen : tagsToOpen) {
      injectOpeningHtmlForRule(tagToOpen, decoratedText);
      charactersReader.registerOpenTag(tagToOpen);
    }
  }

  private static void closeCurrentSyntaxTags(CharactersReader charactersReader, StringBuilder decoratedText) {
    for (int i = 0; i < charactersReader.getOpenTags().size(); i++) {
      injectClosingHtml(decoratedText);
    }
  }

  private static void injectOpeningHtmlForRule(String textType, StringBuilder decoratedText) {
    decoratedText.append("<span class=\"").append(textType).append("\">");
  }

  private static void injectClosingHtml(StringBuilder decoratedText) {
    decoratedText.append("</span>");
  }
}
