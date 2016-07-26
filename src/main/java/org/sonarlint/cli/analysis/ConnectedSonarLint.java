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
package org.sonarlint.cli.analysis;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sonarlint.cli.config.SonarQubeServer;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.util.Logger;
import org.sonarlint.cli.util.SystemInfo;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalUpdateStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ModuleUpdateStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;

public class ConnectedSonarLint extends SonarLint {
  private static final Logger LOGGER = Logger.get();
  private final ConnectedSonarLintEngine engine;
  private final String moduleKey;
  private final SonarQubeServer server;

  public ConnectedSonarLint(ConnectedSonarLintEngine engine, SonarQubeServer server, String moduleKey) {
    this.engine = engine;
    this.server = server;
    this.moduleKey = moduleKey;
  }

  @Override
  public void start(boolean forceUpdate) {
    GlobalUpdateStatus updateStatus = engine.getUpdateStatus();

    if (forceUpdate) {
      LOGGER.info("Updating binding..");
      update();
    } else if (updateStatus == null) {
      LOGGER.info("No binding storage found. Updating..");
      update();
    } else if (updateStatus.isStale()) {
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

    ModuleUpdateStatus moduleUpdateStatus = engine.getModuleUpdateStatus(moduleKey);
    if (moduleUpdateStatus == null) {
      LOGGER.info("Updating data for module..");
      engine.updateModule(getServerConfiguration(server), moduleKey);
      LOGGER.info("Module updated");
    } else if (moduleUpdateStatus.isStale()) {
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

    if (server.token() != null) {
      serverConfigBuilder.token(server.token());
    } else {
      serverConfigBuilder.credentials(server.login(), server.password());
    }
    return serverConfigBuilder.build();
  }

  @Override
  protected void doAnalysis(Map<String, String> properties, ReportFactory reportFactory, List<ClientInputFile> inputFiles, Path baseDirPath) {
    Date start = new Date();
    IssueCollector collector = new IssueCollector();
    ConnectedAnalysisConfiguration config = new ConnectedAnalysisConfiguration(moduleKey, baseDirPath, baseDirPath.resolve(".sonarlint"),
      inputFiles, properties);
    AnalysisResults result = engine.analyze(config, collector);
    generateReports(collector.get(), result, reportFactory, baseDirPath.getFileName().toString(), baseDirPath, start);
  }

  @Override
  public void stop() {
    engine.stop(false);
  }
}
