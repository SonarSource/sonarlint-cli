/*
 * SonarLint CLI
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceProviderTest {
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private SourceProvider sourceProvider;

  @Before
  public void setUp() throws IOException {
    Path testFile = temp.newFile("test").toPath();
    String[] lines = {"line1<html>", "line2", "line3"};

    Files.write(testFile, Arrays.asList(lines), CHARSET);
    sourceProvider = new SourceProvider(temp.getRoot().toPath(), CHARSET);
  }

  @Test
  public void testFindFile() {
    List<String> lines = sourceProvider.getEscapedSource("module1:test");
    assertThat(lines).hasSize(3);
  }

  @Test
  public void testInvalidFile() {
    List<String> lines = sourceProvider.getEscapedSource("module1:testINVALID");
    assertThat(lines).isEmpty();
  }

  @Test
  public void testEscapeHtml() {
    List<String> lines = sourceProvider.getEscapedSource("module1:test");
    assertThat(lines.get(0)).isEqualTo("line1&lt;html&gt;");
  }
}
