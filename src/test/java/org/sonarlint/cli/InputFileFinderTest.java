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
package org.sonarlint.cli;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class InputFileFinderTest {
  private Path root;
  private Path src1;
  private Path test1;
  private InputFileFinder fileFinder;

  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    setUpLogger();
    root = temp.getRoot().toPath();
    Path src = root.resolve("src");
    Path tests = root.resolve("tests");

    test1 = tests.resolve("Test.java");
    src1 = src.resolve("Src.java");

    Files.createDirectories(src);
    Files.createDirectories(tests);

    Files.createFile(test1);
    Files.createFile(src1);
  }

  private void setUpLogger() {
    out = new ByteArrayOutputStream();
    PrintStream outStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    PrintStream errStream = new PrintStream(err);
    Logger.set(outStream, errStream);
  }

  private String getLogs(ByteArrayOutputStream stream) {
    return new String(stream.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  public void onlyTest() throws IOException {
    fileFinder = new InputFileFinder(null, "**tests**", Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("test").contains(true, false);
  }

  @Test
  public void invalidPattern() throws IOException {
    exception.expect(PatternSyntaxException.class);
    try {
      fileFinder = new InputFileFinder(null, "\\", Charset.defaultCharset());
    } catch (Exception e) {
      err.flush();
      assertThat(getLogs(err)).contains("Error creating matcher with pattern: \\");
      throw e;
    }
  }

  @Test
  public void onlySrc() throws IOException {
    fileFinder = new InputFileFinder("**src**", null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(1);
    assertThat(files).extracting("test").containsOnly(false);

    assertThat(files.get(0).getPath()).isEqualTo(src1);
    assertThat(files.get(0).getCharset()).isEqualTo(Charset.defaultCharset());
  }

  @Test
  public void testDefault() throws IOException {
    fileFinder = new InputFileFinder(null, null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("test").containsOnly(false);
  }

  @Test
  public void testOverlapping() throws IOException {
    fileFinder = new InputFileFinder("**tests**", "**tests**", Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(1);
    assertThat(files).extracting("test").containsOnly(true);
  }

  @Test
  public void testIgnoreHidden() throws IOException {
    fileFinder = new InputFileFinder(null, null, Charset.defaultCharset());
    File hiddenFolder = temp.newFolder(".test");
    Path hiddenSrc = hiddenFolder.toPath().resolve("Test.java");
    List<ClientInputFile> files = fileFinder.collect(root);

    assertThat(files).extracting("path").doesNotContain(hiddenSrc);
    assertThat(files).extracting("path").contains(src1);
  }

  @Test
  public void testDisjoint() throws IOException {
    fileFinder = new InputFileFinder("**abc**", "**abc**", Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).isEmpty();
  }

  @Test
  public void testCharset() throws IOException {
    fileFinder = new InputFileFinder(null, null, StandardCharsets.US_ASCII);

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("charset").containsOnly(StandardCharsets.US_ASCII);
  }

}
