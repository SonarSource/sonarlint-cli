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
import org.sonarsource.sonarlint.core.AnalysisConfiguration.InputFile;

import javax.annotation.Nullable;

import java.io.IOException;
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
  private Logger logger;
  private PathMatcher srcMatcher;
  private PathMatcher testsMatcher;

  public InputFileFinder(@Nullable String srcGlobPattern, @Nullable String testsGlobPattern) {
    logger = Logger.get();

    try {
      if (srcGlobPattern != null) {
        srcMatcher = FileSystems.getDefault().getPathMatcher("glob:" + srcGlobPattern);
      } else {
        srcMatcher = acceptAll;
      }
    } catch (Exception e) {
      logger.error("Error creating matcher with pattern: " + srcGlobPattern);
      throw e;
    }

    try {
      if (testsGlobPattern != null) {
        testsMatcher = FileSystems.getDefault().getPathMatcher("glob:" + testsGlobPattern);
      } else {
        testsMatcher = refuseAll;
      }
    } catch (Exception e) {
      logger.error("Error creating matcher with pattern: " + testsGlobPattern);
      throw e;
    }
  }

  public List<InputFile> collect(Path dir) throws IOException {
    final List<InputFile> files = new ArrayList<>();

    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
        boolean isTest = testsMatcher.matches(file);
        boolean isSrc = srcMatcher.matches(file);

        if (isTest || isSrc) {
          files.add(new DefaultInputFile(file, isTest));
        }

        return super.visitFile(file, attrs);
      }
    });

    return files;
  }

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

  private static class DefaultInputFile implements InputFile {
    private final Path path;
    private final boolean test;

    DefaultInputFile(Path path, boolean test) {
      this.path = path;
      this.test = test;
    }

    @Override
    public Path path() {
      return path;
    }

    @Override
    public boolean isTest() {
      return test;
    }
  }

}
