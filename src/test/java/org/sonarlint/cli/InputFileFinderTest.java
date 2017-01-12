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
package org.sonarlint.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class InputFileFinderTest {
  private Path root;
  private Path src1;
  private Path test1;
  private InputFileFinder fileFinder;

  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
  public void onlyTestByAbsolutePath() throws IOException {
    onlyTest("**tests**");
  }

  @Test
  public void onlyTestByRelativePath() throws IOException {
    onlyTest("tests**");
  }

  public void onlyTest(String pattern) throws IOException {
    fileFinder = new InputFileFinder(null, pattern, null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("test").contains(true, false);
  }

  @Test
  public void invalidSourcePattern() throws IOException {
    try {
      fileFinder = new InputFileFinder("\\", null, null, Charset.defaultCharset());
      fail("Expected exception");
    } catch (Exception e) {
      err.flush();
      assertThat(getLogs(err)).contains("Error creating matcher for sources with pattern: \\");
    }
  }

  @Test
  public void invalidTestPattern() throws IOException {
    try {
      fileFinder = new InputFileFinder(null, "\\", null, Charset.defaultCharset());
      fail("Expected exception");
    } catch (Exception e) {
      err.flush();
      assertThat(getLogs(err)).contains("Error creating matcher for tests with pattern: \\");
    }
  }

  @Test
  public void invalidExclusionPattern() throws IOException {
    try {
      fileFinder = new InputFileFinder(null, null, "\\", Charset.defaultCharset());
      fail("Expected exception");
    } catch (Exception e) {
      err.flush();
      assertThat(getLogs(err)).contains("Error creating matcher for exclusions with pattern: \\");
    }
  }

  @Test
  public void onlySrcByAbsolutePath() throws IOException {
    onlySrc("**src**");
  }

  @Test
  public void onlySrcByRelativePath() throws IOException {
    onlySrc("src**");
  }

  public void onlySrc(String pattern) throws IOException {
    fileFinder = new InputFileFinder(pattern, null, null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(1);
    assertThat(files).extracting("test").containsOnly(false);

    assertThat(files.get(0).getPath()).isEqualTo(src1.toString());
    assertThat(files.get(0).getCharset()).isEqualTo(Charset.defaultCharset());
  }

  @Test
  public void testDefault() throws IOException {
    fileFinder = new InputFileFinder(null, null, null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("test").containsOnly(false);
  }

  @Test
  public void testOverlapping() throws IOException {
    fileFinder = new InputFileFinder("**tests**", "**tests**", null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(1);
    assertThat(files).extracting("test").containsOnly(true);
  }

  @Test
  public void testIgnoreHidden() throws IOException {
    fileFinder = new InputFileFinder(null, null, null, Charset.defaultCharset());
    File hiddenFolder = temp.newFolder(".test");
    Path hiddenSrc = hiddenFolder.toPath().resolve("Test.java");
    List<ClientInputFile> files = fileFinder.collect(root);

    assertThat(files).extracting("path").doesNotContain(hiddenSrc);
    assertThat(files).extracting("path").contains(src1.toString());
  }

  @Test
  public void testDisjoint() throws IOException {
    fileFinder = new InputFileFinder("**abc**", "**abc**", null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).isEmpty();
  }

  @Test
  public void testCharset() throws IOException {
    fileFinder = new InputFileFinder(null, null, null, StandardCharsets.US_ASCII);

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).hasSize(2);
    assertThat(files).extracting("charset").containsOnly(StandardCharsets.US_ASCII);
  }

  @Test
  public void testPatternAppliedToSourceFilesOnly() throws Exception {
    Path src = root.resolve("src");

    test1 = src.resolve("FirstTest.java");
    Path test2 = src.resolve("SecondTest.java");
    Path externalTest = root.resolve("ExternalTest.java");

    Files.createDirectories(src);

    Files.createFile(test1);
    Files.createFile(test2);
    Files.createFile(externalTest);

    fileFinder = new InputFileFinder("src/**", "*Test.*", null, Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).extracting("path").containsOnly(test1.toString(), test2.toString(), src1.toString());
  }

  @Test
  public void testExclusions() throws IOException {
    fileFinder = new InputFileFinder(null, null, "tests/**", Charset.defaultCharset());

    List<ClientInputFile> files = fileFinder.collect(root);
    assertThat(files).extracting("path").containsOnly(src1.toString());
  }

}
