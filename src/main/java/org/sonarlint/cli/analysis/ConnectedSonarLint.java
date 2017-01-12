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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonarlint.cli.config.SonarQubeServer;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.util.Logger;
import org.sonarlint.cli.util.SystemInfo;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ModuleStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.tracking.CachingIssueTracker;
import org.sonarsource.sonarlint.core.tracking.CachingIssueTrackerImpl;
import org.sonarsource.sonarlint.core.tracking.Console;
import org.sonarsource.sonarlint.core.tracking.InMemoryIssueTrackerCache;
import org.sonarsource.sonarlint.core.tracking.IssueTrackable;
import org.sonarsource.sonarlint.core.tracking.IssueTrackerCache;
import org.sonarsource.sonarlint.core.tracking.ServerIssueTracker;
import org.sonarsource.sonarlint.core.tracking.Trackable;

import static org.sonarsource.sonarlint.core.client.api.util.FileUtils.toSonarQubePath;

public class ConnectedSonarLint extends SonarLint {
  private static final Logger LOGGER = Logger.get();

  private final ConnectedSonarLintEngine engine;
  private final String moduleKey;
  private final SonarQubeServer server;

  ConnectedSonarLint(ConnectedSonarLintEngine engine, SonarQubeServer server, String moduleKey) {
    this.engine = engine;
    this.server = server;
    this.moduleKey = moduleKey;
  }

  @Override
  public void start(boolean forceUpdate) {
    GlobalStorageStatus globalStorageStatus = engine.getGlobalStorageStatus();

    if (forceUpdate) {
      LOGGER.info("Updating binding..");
      update();
    } else if (globalStorageStatus == null) {
      LOGGER.info("No binding storage found. Updating..");
      update();
    } else if (globalStorageStatus.isStale()) {
      LOGGER.info("Binding storage is stale. Updating..");
      update();
    } else {
      checkModuleStatus();
    }
  }

  private void checkModuleStatus() {
    engine.allModulesByKey().keySet().stream()
      .filter(key -> key.equals(moduleKey))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Project key '" + moduleKey + "' not found in the binding storage. Maybe an update of the storage is needed with '-u'?"));

    ModuleStorageStatus moduleStorageStatus = engine.getModuleStorageStatus(moduleKey);
    if (moduleStorageStatus == null) {
      LOGGER.info("Updating data for module..");
      engine.updateModule(getServerConfiguration(server), moduleKey);
      LOGGER.info("Module updated");
    } else if (moduleStorageStatus.isStale()) {
      LOGGER.info("Module's data is stale. Updating..");
      engine.updateModule(getServerConfiguration(server), moduleKey);
      LOGGER.info("Module updated");
    }
  }

  private void update() {
    engine.update(getServerConfiguration(server));
    engine.allModulesByKey().keySet().stream()
      .filter(key -> key.equals(moduleKey))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("Project key '" + moduleKey + "' not found in the SonarQube server"));
    updateModule();
    LOGGER.info("Binding updated");
  }

  private void updateModule() {
    engine.updateModule(getServerConfiguration(server), moduleKey);
  }

  private static ServerConfiguration getServerConfiguration(SonarQubeServer server) {
    ServerConfiguration.Builder serverConfigBuilder = ServerConfiguration.builder()
      .url(server.url())
      .userAgent("SonarLint CLI " + SystemInfo.getVersion());

    String token = server.token();
    if (token != null) {
      serverConfigBuilder.token(token);
    } else {
      serverConfigBuilder.credentials(server.login(), server.password());
    }
    return serverConfigBuilder.build();
  }

  @Override
  protected void doAnalysis(Map<String, String> properties, ReportFactory reportFactory, List<ClientInputFile> inputFiles, Path baseDirPath) {
    Date start = new Date();
    ConnectedAnalysisConfiguration config = new ConnectedAnalysisConfiguration(moduleKey, baseDirPath, baseDirPath.resolve(".sonarlint"),
      inputFiles, properties);
    IssueCollector collector = new IssueCollector();
    AnalysisResults result = engine.analyze(config, collector);
    engine.downloadServerIssues(getServerConfiguration(server), moduleKey);
    Collection<Trackable> trackables = matchAndTrack(baseDirPath, collector.get());
    generateReports(trackables, result, reportFactory, baseDirPath.getFileName().toString(), baseDirPath, start);
  }

  Collection<Trackable> matchAndTrack(Path baseDirPath, Collection<Issue> issues) {
    Collection<Issue> issuesWithFile = issues.stream().filter(issue -> issue.getInputFile() != null).collect(Collectors.toList());
    Collection<String> relativePaths = getRelativePaths(baseDirPath, issuesWithFile);
    Map<String, List<Trackable>> trackablesPerFile = getTrackablesPerFile(baseDirPath, issuesWithFile);
    IssueTrackerCache cache = createCurrentIssueTrackerCache(relativePaths, trackablesPerFile);
    return getCurrentTrackables(relativePaths, cache);
  }

  private IssueTrackerCache createCurrentIssueTrackerCache(Collection<String> relativePaths, Map<String, List<Trackable>> trackablesPerFile) {
    IssueTrackerCache cache = new InMemoryIssueTrackerCache();
    CachingIssueTracker issueTracker = new CachingIssueTrackerImpl(cache);
    trackablesPerFile.entrySet().forEach(entry -> issueTracker.matchAndTrackAsNew(entry.getKey(), entry.getValue()));
    ServerIssueTracker serverIssueTracker = new ServerIssueTracker(new MyLogger(), new MyConsole(), issueTracker);
    serverIssueTracker.update(engine, moduleKey, relativePaths);
    return cache;
  }

  private static List<Trackable> getCurrentTrackables(Collection<String> relativePaths, IssueTrackerCache cache) {
    return relativePaths.stream().flatMap(f -> cache.getCurrentTrackables(f).stream())
      .filter(trackable -> !trackable.isResolved())
      .collect(Collectors.toList());
  }

  private Map<String, List<Trackable>> getTrackablesPerFile(Path baseDirPath, Collection<Issue> issues) {
    return issues.stream()
      .collect(Collectors.groupingBy(issue -> getRelativePath(baseDirPath, issue), Collectors.toList()))
      .entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> entry.getValue().stream()
          .map(IssueTrackable::new)
          .collect(Collectors.toCollection(ArrayList::new))));
  }

  private Collection<String> getRelativePaths(Path baseDirPath, Collection<Issue> issues) {
    return issues.stream()
        .map(issue -> getRelativePath(baseDirPath, issue))
        .collect(Collectors.toSet());
  }

  // note: engine.downloadServerIssues correctly figures out correct moduleKey and fileKey
  @CheckForNull
  String getRelativePath(Path baseDirPath, Issue issue) {
    ClientInputFile inputFile = issue.getInputFile();
    if (inputFile == null) {
      return null;
    }

    return toSonarQubePath(baseDirPath.relativize(Paths.get(inputFile.getPath())).toString());
  }

  @Override
  protected RuleDetails getRuleDetails(String ruleKey) {
    return engine.getRuleDetails(ruleKey);
  }

  @Override
  public void stop() {
    engine.stop(false);
  }

  private static class MyLogger implements org.sonarsource.sonarlint.core.tracking.Logger {
    @Override public void error(String message, Exception e) {
      LOGGER.error(message, e);
    }

    @Override public void debug(String message, Exception e) {
      LOGGER.debug(message, e);
    }

    @Override public void debug(String message) {
      LOGGER.debug(message);
    }
  }

  private static class MyConsole implements Console {
    @Override public void info(String message) {
      LOGGER.info(message);
    }

    @Override public void error(String message, Throwable t) {
      LOGGER.error(message, t);
    }
  }
}
