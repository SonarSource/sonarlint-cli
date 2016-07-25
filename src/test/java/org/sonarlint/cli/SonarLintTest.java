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
import org.sonarlint.cli.report.ReportFactory;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarLintTest {
  private SonarLint sonarLint;
  private StandaloneSonarLintEngine client;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws IOException {
    client = new StandaloneSonarLintEngineImpl(StandaloneGlobalConfiguration.builder().build());
    sonarLint = new SonarLint(client);
  }

  @Test
  public void startStop() {
    assertThat(sonarLint.isRunning()).isTrue();
    sonarLint.stop();
    assertThat(sonarLint.isRunning()).isFalse();
  }

  @Test
  public void run() throws IOException {
    InputFileFinder fileFinder = mock(InputFileFinder.class);
    Path inputFile = temp.newFile().toPath();
    when(fileFinder.collect(any(Path.class))).thenReturn(Collections.singletonList(createInputFile(inputFile, false)));
    String path = temp.newFolder().getAbsolutePath();
    System.setProperty(SonarProperties.PROJECT_HOME, path);
    sonarLint.runAnalysis(new Options(), new ReportFactory(StandardCharsets.UTF_8), fileFinder);

    verify(fileFinder).collect(Paths.get(path));

    Path htmlReport = Paths.get(path).resolve(".sonarlint").resolve("sonarlint-report.html");
    assertThat(htmlReport).exists();
  }

  @Test
  public void runWithoutFiles() throws IOException {
    InputFileFinder fileFinder = mock(InputFileFinder.class);
    when(fileFinder.collect(any(Path.class))).thenReturn(Collections.<ClientInputFile>emptyList());
    String path = temp.newFolder().getAbsolutePath();
    System.setProperty(SonarProperties.PROJECT_HOME, path);

    sonarLint.runAnalysis(new Options(), new ReportFactory(StandardCharsets.UTF_8), fileFinder);

    Path htmlReport = Paths.get(path).resolve(".sonarlint").resolve("sonarlint-report.html");
    assertThat(htmlReport).doesNotExist();
  }

  @Test
  public void create() throws IOException {
    temp.newFolder("plugins");
    System.setProperty(SonarProperties.SONARLINT_HOME, temp.getRoot().getAbsolutePath());

    SonarLint sl = SonarLint.create(new Options());
    assertThat(sl).isNotNull();
    assertThat(sl.isRunning()).isTrue();
  }

  @Test
  public void errorLoadingPlugins() throws IOException {
    System.clearProperty(SonarProperties.SONARLINT_HOME);
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Can't find SonarLint home. System property not set: ");
    SonarLint.loadPlugins();
  }

  @Test
  public void loadPlugins() throws IOException, URISyntaxException {
    File dir = temp.newFolder("plugins");
    File plugin = new File(dir, "test.jar");
    plugin.createNewFile();
    System.setProperty(SonarProperties.SONARLINT_HOME, temp.getRoot().getAbsolutePath());

    URL[] plugins = SonarLint.loadPlugins();
    assertThat(plugins).hasSize(1);
    assertThat(new File(plugins[0].toURI()).getAbsolutePath()).isEqualTo(plugin.getAbsolutePath());
  }

  @Test
  public void testIssueCollector() {
    SonarLint.IssueCollector collector = new SonarLint.IssueCollector();
    Issue i1 = createIssue("rule1");
    Issue i2 = createIssue("rule2");

    collector.handle(i1);
    collector.handle(i2);

    assertThat(collector.get()).containsExactly(i1, i2);
  }

  private static ClientInputFile createInputFile(final Path filePath, final boolean test) {
    return new ClientInputFile() {

      @Override
      public Path getPath() {
        return filePath;
      }

      @Override
      public boolean isTest() {
        return test;
      }

      @Override
      public Charset getCharset() {
        return StandardCharsets.UTF_8;
      }

      @Override
      public <G> G getClientObject() {
        return null;
      }
    };
  }

  private static Issue createIssue(String ruleKey) {
    Issue i = mock(Issue.class);
    when(i.getRuleKey()).thenReturn(ruleKey);
    return i;
  }
}
