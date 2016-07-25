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

import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class InputFileFinder {
  private static final Logger LOGGER = Logger.get();
  private final PathMatcher srcMatcher;
  private final PathMatcher testsMatcher;
  private final Charset charset;

  private static PathMatcher acceptAll = new PathMatcher() {
    @Override
    public boolean matches(Path path) {
      return true;
    }
  };

  private static PathMatcher refuseAll = new PathMatcher() {
    @Override
    public boolean matches(Path path) {
      return false;
    }
  };

  public InputFileFinder(@Nullable String srcGlobPattern, @Nullable String testsGlobPattern, Charset charset) {
    this.charset = charset;
    FileSystem fs = FileSystems.getDefault();
    try {
      if (srcGlobPattern != null) {
        srcMatcher = fs.getPathMatcher("glob:" + srcGlobPattern);
      } else {
        srcMatcher = acceptAll;
      }
    } catch (Exception e) {
      LOGGER.error("Error creating matcher with pattern: " + srcGlobPattern);
      throw e;
    }

    try {
      if (testsGlobPattern != null) {
        testsMatcher = fs.getPathMatcher("glob:" + testsGlobPattern);
      } else {
        testsMatcher = refuseAll;
      }
    } catch (Exception e) {
      LOGGER.error("Error creating matcher with pattern: " + testsGlobPattern);
      throw e;
    }
  }

  public List<ClientInputFile> collect(Path dir) throws IOException {
    final List<ClientInputFile> files = new ArrayList<>();
    Files.walkFileTree(dir, new FileCollector(files));
    return files;
  }

  private class FileCollector extends SimpleFileVisitor<Path> {
    private List<ClientInputFile> files;

    private FileCollector(List<ClientInputFile> files) {
      this.files = files;
    }

    @Override
    public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
      boolean isTest = testsMatcher.matches(file);
      boolean isSrc = srcMatcher.matches(file);

      if (isTest || isSrc) {
        files.add(new DefaultClientInputFile(file, isTest, charset));
      }

      return super.visitFile(file, attrs);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (Files.isHidden(dir)) {
        LOGGER.debug("Ignoring hidden directory: " + dir.toString());
        return FileVisitResult.SKIP_SUBTREE;
      }

      return super.preVisitDirectory(dir, attrs);
    }
  }

  private static class DefaultClientInputFile implements ClientInputFile {
    private final Path path;
    private final boolean test;
    private final Charset charset;

    DefaultClientInputFile(Path path, boolean test, Charset charset) {
      this.path = path;
      this.test = test;
      this.charset = charset;
    }

    @Override
    public boolean isTest() {
      return test;
    }

    @Override
    public Path getPath() {
      return path;
    }

    @Override
    public Charset getCharset() {
      return charset;
    }

    @Override
    public <G> G getClientObject() {
      return null;
    }
  }
}
