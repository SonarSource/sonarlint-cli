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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonarlint.cli.SonarProperties;
import org.sonarlint.cli.config.ConfigurationReader;
import org.sonarlint.cli.config.GlobalConfiguration;
import org.sonarlint.cli.config.ProjectConfiguration;
import org.sonarlint.cli.config.SonarQubeServer;
import org.sonarlint.cli.util.Logger;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;

public class SonarLintFactory {
  private static final Logger LOGGER = Logger.get();

  private static final Path GLOBAL_CONFIGURATION_FILEPATH;

  private static final String GLOBAL_CONFIGURATION_FILENAME = "global.json";
  private static final String PROJECT_CONFIGURATION_FILENAME = "sonarlint.json";

  private final ConfigurationReader configurationReader;

  static {
    String home = System.getProperty("user.home");
    GLOBAL_CONFIGURATION_FILEPATH = Paths.get(home)
      .resolve(".sonarlint")
      .resolve("conf")
      .resolve(GLOBAL_CONFIGURATION_FILENAME);
  }

  public SonarLintFactory(ConfigurationReader configurationReader) {
    this.configurationReader = configurationReader;
  }

  public SonarLint createSonarLint(Path projectHome, boolean mustBeConnected, boolean verbose) {
    return createSonarLint(GLOBAL_CONFIGURATION_FILEPATH, projectHome.resolve(PROJECT_CONFIGURATION_FILENAME), mustBeConnected, verbose);
  }

  public SonarLint createSonarLint(Path globalConfigPath, Path projectConfigPath, boolean mustBeConnected, boolean verbose) {
    if (!Files.exists(projectConfigPath)) {
      if (mustBeConnected) {
        throw new IllegalStateException("Can't update project - no binding defined in: " + projectConfigPath.toAbsolutePath());
      }
      return createStandalone(verbose);
    }

    ProjectConfiguration project = configurationReader.readProject(projectConfigPath);

    if (!Files.exists(globalConfigPath)) {
      throw new IllegalStateException("Found project binding but there is no SonarQube server configured in: " + globalConfigPath.toAbsolutePath());
    }

    GlobalConfiguration global = configurationReader.readGlobal(globalConfigPath);
    List<SonarQubeServer> servers = getServers(global);

    String projectKey = project.projectKey();
    SonarQubeServer server;

    if (project.serverId() == null) {
      if (servers.size() > 1) {
        throw new IllegalStateException(
          String.format("No SonarQube server id is defined in the project binding (%s) and there are multiple servers defined in the global configuration",
            projectConfigPath.toAbsolutePath()));
      }
      server = servers.get(0);
    } else {
      Optional<SonarQubeServer> optionalServer = servers.stream().filter(s -> s.id().equals(project.serverId())).findFirst();
      server = optionalServer
        .orElseThrow(() -> new IllegalStateException(String.format("No SonarQube server configuration found in '%s' for the server id defined in the project binding: '%s'",
          globalConfigPath.toAbsolutePath(), project.serverId())));
    }

    return createConnected(server, projectKey, verbose);
  }

  private static List<SonarQubeServer> getServers(GlobalConfiguration conf) {
    List<SonarQubeServer> servers = conf.servers();
    if (servers == null) {
      return Collections.emptyList();
    }

    return servers;
  }

  private static SonarLint createConnected(SonarQubeServer server, String projectKey, boolean verbose) {
    LOGGER.info(String.format("Connected mode (%s)", projectKey));
    ConnectedGlobalConfiguration config = ConnectedGlobalConfiguration.builder()
      .setLogOutput(new DefaultLogOutput(LOGGER, verbose))
      .setServerId(server.id())
      .build();
    ConnectedSonarLintEngineImpl engine = new ConnectedSonarLintEngineImpl(config);
    return new ConnectedSonarLint(engine, server, projectKey);
  }

  private static SonarLint createStandalone(boolean verbose) {
    LOGGER.info("Standalone mode");
    URL[] plugins;

    try {
      plugins = loadPlugins();
    } catch (Exception e) {
      throw new IllegalStateException("Error loading plugins", e);
    }

    StandaloneGlobalConfiguration config = StandaloneGlobalConfiguration.builder()
      .addPlugins(plugins)
      .setLogOutput(new DefaultLogOutput(LOGGER, verbose))
      .build();

    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(config);
    return new StandaloneSonarLint(engine);
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

}
