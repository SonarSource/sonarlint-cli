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
package org.sonarlint.cli.analysis;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.cli.InputFileFinder;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StandaloneSonarLintTest {
  private StandaloneSonarLint sonarLint;
  private StandaloneSonarLintEngine engine;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    engine = new StandaloneSonarLintEngineImpl(StandaloneGlobalConfiguration.builder().build());
    sonarLint = new StandaloneSonarLint(engine);
  }

  @Test
  public void startStop() {
    engine = mock(StandaloneSonarLintEngine.class);
    sonarLint = new StandaloneSonarLint(engine);
    sonarLint.stop();
    verify(engine).stop();
  }

  @Test
  public void run() throws IOException {
    InputFileFinder fileFinder = mock(InputFileFinder.class);
    Path inputFile = temp.newFile().toPath();
    when(fileFinder.collect(any(Path.class))).thenReturn(Collections.singletonList(createInputFile(inputFile, false)));
    Path projectHome = temp.newFolder().toPath();
    sonarLint.runAnalysis(new HashMap<>(), new ReportFactory(StandardCharsets.UTF_8), fileFinder, projectHome);

    verify(fileFinder).collect(projectHome);

    Path htmlReport = projectHome.resolve(".sonarlint").resolve("sonarlint-report.html");
    assertThat(htmlReport).exists();
  }

  @Test
  public void runWithoutFiles() throws IOException {
    InputFileFinder fileFinder = mock(InputFileFinder.class);
    when(fileFinder.collect(any(Path.class))).thenReturn(Collections.emptyList());
    Path projectHome = temp.newFolder().toPath();
    sonarLint.runAnalysis(new HashMap<>(), new ReportFactory(StandardCharsets.UTF_8), fileFinder, projectHome);

    Path htmlReport = projectHome.resolve(".sonarlint").resolve("sonarlint-report.html");
    assertThat(htmlReport).doesNotExist();
  }

  private static ClientInputFile createInputFile(final Path filePath, final boolean test) {
    return new InputFileFinder.DefaultClientInputFile(filePath, test, StandardCharsets.UTF_8);
  }
}
