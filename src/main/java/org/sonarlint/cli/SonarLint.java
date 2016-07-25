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

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.report.Reporter;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

import static org.sonarlint.cli.SonarProperties.PROJECT_HOME;

public class SonarLint {
  private static final Logger LOGGER = Logger.get();
  private boolean running;
  private final StandaloneSonarLintEngine engine;

  public SonarLint(StandaloneSonarLintEngine engine) throws IOException {
    this.engine = engine;
    this.running = true;
  }

  public static SonarLint create(Options opts) throws IOException {
    URL[] plugins;

    try {
      plugins = loadPlugins();
    } catch (Exception e) {
      throw new IllegalStateException("Error loading plugins", e);
    }

    StandaloneGlobalConfiguration config = StandaloneGlobalConfiguration.builder()
      .addPlugins(plugins)
      .setLogOutput(new DefaultLogOutput(LOGGER))
      // verbose?
      .build();

    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(config);
    return new SonarLint(engine);
  }

  @VisibleForTesting
  static URL[] loadPlugins() throws IOException {
    String sonarlintHome = System.getProperty(SonarProperties.SONARLINT_HOME);

    if (sonarlintHome == null) {
      throw new IllegalStateException("Can't find SonarLint home. System property not set: " + SonarProperties.SONARLINT_HOME);
    }

    Path sonarLintHomePath = Paths.get(sonarlintHome);
    Path pluginDir = sonarLintHomePath.resolve("plugins");

    List<URL> pluginsUrls = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginDir)) {
      for (Path path : directoryStream) {
        pluginsUrls.add(path.toUri().toURL());
      }
    }
    return pluginsUrls.toArray(new URL[pluginsUrls.size()]);
  }

  public void runAnalysis(Options opts, ReportFactory reportFactory, InputFileFinder finder) throws IOException {
    Date start = new Date();
    String baseDir = System.getProperty(PROJECT_HOME);

    Path baseDirPath = Paths.get(baseDir);
    List<ClientInputFile> inputFiles = finder.collect(baseDirPath);

    if (inputFiles.isEmpty()) {
      LOGGER.warn("No files to analyze");
      return;
    } else {
      LOGGER.debug(String.format("Submitting %d files for analysis", inputFiles.size()));
    }

    IssueCollector collector = new IssueCollector();
    AnalysisResults result = engine.analyze(new StandaloneAnalysisConfiguration(baseDirPath, baseDirPath.resolve(".sonarlint"), inputFiles, toMap(opts.properties())), collector);
    generateReports(collector.get(), result, reportFactory, baseDirPath.getFileName().toString(), baseDirPath, start);
  }

  private static Map<String, String> toMap(Properties properties) {
    return new HashMap<>((Map) properties);
  }

  private static void generateReports(List<Issue> issues, AnalysisResults result, ReportFactory reportFactory, String projectName, Path baseDir, Date date) {
    List<Reporter> reporters = reportFactory.createReporters(baseDir);

    for (Reporter r : reporters) {
      r.execute(projectName, date, issues, result);
    }
  }

  public void stop() {
    engine.stop();
    running = false;
  }

  public boolean isRunning() {
    return running;
  }

  static class IssueCollector implements IssueListener {
    private List<Issue> issues = new LinkedList<>();

    @Override
    public void handle(Issue issue) {
      issues.add(issue);
    }

    public List<Issue> get() {
      return issues;
    }
  }
}
