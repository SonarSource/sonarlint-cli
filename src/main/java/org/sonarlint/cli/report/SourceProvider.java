/*
 * SonarLint CLI
 * Copyright (C) 2016 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.cli.report;

import org.sonarlint.cli.util.HtmlEntities;
import org.sonarlint.cli.util.Logger;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceProvider {
  private final Logger logger = Logger.get();
  private final Path basePath;
  private final Charset charset;

  public SourceProvider(Path basePath, Charset charset) {
    this.basePath = basePath;
    this.charset = charset;
  }

  public List<String> getEscapedSource(String componentKey) {
    try {
      Path filePath = getResource(componentKey);
      if (filePath == null) {
        // invalid, directory, project issue, ...
        return Collections.emptyList();
      }

      List<String> lines = Files.readAllLines(filePath, charset);
      List<String> escapedLines = new ArrayList<>(lines.size());
      for (String line : lines) {
        escapedLines.add(HtmlEntities.encode(line));
      }
      return escapedLines;
    } catch (IOException e) {
      logger.error("Unable to read source code of resource: " + componentKey, e);
      return Collections.emptyList();
    }
  }

  @CheckForNull
  private Path getResource(String componentKey) {
    String[] split = componentKey.split(":");
    if (split.length != 2) {
      return null;
    }

    Path filePath = basePath.resolve(split[1]);
    if (!Files.isRegularFile(filePath)) {
      return null;
    }

    return filePath;
  }

}
