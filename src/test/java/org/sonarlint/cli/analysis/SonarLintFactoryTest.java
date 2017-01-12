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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.cli.SonarProperties;
import org.sonarlint.cli.config.ConfigurationReader;
import org.sonarlint.cli.config.GlobalConfiguration;
import org.sonarlint.cli.config.ProjectConfiguration;
import org.sonarlint.cli.config.SonarQubeServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarLintFactoryTest {
  private ConfigurationReader reader;
  private Path globalConfigPath;
  private Path projectConfigPath;
  private SonarLintFactory sonarLintFactory;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    System.setProperty(SonarProperties.SONARLINT_HOME, temp.getRoot().getAbsolutePath());
    reader = mock(ConfigurationReader.class);
    sonarLintFactory = new SonarLintFactory(reader);
    temp.newFolder("plugins");
  }

  private void mockConfigs(@Nullable GlobalConfiguration global, @Nullable ProjectConfiguration project) {
    try {
      globalConfigPath = temp.newFile().toPath();
      if (global != null) {
        when(reader.readGlobal(any(Path.class))).thenReturn(global);
      } else {
        Files.delete(globalConfigPath);
      }

      projectConfigPath = temp.newFile().toPath();
      if (project != null) {
        when(reader.readProject(any(Path.class))).thenReturn(project);
      } else {
        Files.delete(projectConfigPath);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  public void test_createSonarLint_with_custom_global_config() {
    mockConfigs(null, null);
    SonarLint sonarLint = sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);

    assertThat(sonarLint).isNotNull();
    assertThat(sonarLint).isInstanceOf(StandaloneSonarLint.class);
  }

  @Test
  public void test_createSonarLint_with_default_global_config() {
    SonarLint sonarLint = sonarLintFactory.createSonarLint(Paths.get("dummy"), false, true);

    assertThat(sonarLint).isNotNull();
    assertThat(sonarLint).isInstanceOf(StandaloneSonarLint.class);
  }

  @Test
  public void failIfUpdateAndStandalone() {
    mockConfigs(null, null);
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Can't update project - no binding defined");
    sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, true, true);
  }

  @Test
  public void testCreateConnected() {
    GlobalConfiguration global = createGlobalConfig("localhost");
    ProjectConfiguration project = createProjectConfig("localhost", "project1");
    mockConfigs(global, project);
    SonarLint sonarLint = sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);

    assertThat(sonarLint).isNotNull();
    assertThat(sonarLint).isInstanceOf(ConnectedSonarLint.class);
  }

  @Test
  public void testCreateConnectedWithoutExplicitServer() {
    GlobalConfiguration global = createGlobalConfig("localhost");
    ProjectConfiguration project = createProjectConfig(null, "project1");
    mockConfigs(global, project);
    SonarLint sonarLint = sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);

    assertThat(sonarLint).isNotNull();
    assertThat(sonarLint).isInstanceOf(ConnectedSonarLint.class);
  }

  @Test
  public void failIfServerNotFound() {
    GlobalConfiguration global = createGlobalConfig("localhost");
    ProjectConfiguration project = createProjectConfig("localhost2", "project1");
    mockConfigs(global, project);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("No SonarQube server configuration found");
    sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);
  }

  @Test
  public void failIfSeveralServerOptions() {
    GlobalConfiguration global = createGlobalConfig("localhost", "localhost2");
    ProjectConfiguration project = createProjectConfig(null, "project1");
    mockConfigs(global, project);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("there are multiple servers defined in the global configuration");
    sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);
  }

  @Test
  public void failIfNoGlobalConfig() {
    ProjectConfiguration project = createProjectConfig("localhost2", "project1");
    mockConfigs(null, project);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("there is no SonarQube server configured in");
    sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);
  }

  public void failIfMultipleServers() {
    GlobalConfiguration global = createGlobalConfig("localhost", "localhost2");
    ProjectConfiguration project = createProjectConfig(null, "project1");
    mockConfigs(global, project);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("No SonarQube server URL is defined in the project binding");
    sonarLintFactory.createSonarLint(globalConfigPath, projectConfigPath, false, true);
  }

  @Test
  public void errorLoadingPlugins() throws IOException {
    System.clearProperty(SonarProperties.SONARLINT_HOME);
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Can't find SonarLint home. System property not set: ");
    SonarLintFactory.loadPlugins();
  }

  @Test
  public void loadPlugins() throws IOException, URISyntaxException {
    Path plugin = temp.getRoot().toPath().resolve("plugins").resolve("test.jar");
    Files.createFile(plugin);

    URL[] plugins = SonarLintFactory.loadPlugins();
    assertThat(plugins).hasSize(1);
    assertThat(Paths.get(plugins[0].toURI())).isEqualTo(plugin);
  }

  private GlobalConfiguration createGlobalConfig(String... serverIds) {
    List<SonarQubeServer> servers = new LinkedList<>();

    for (String id : serverIds) {
      SonarQubeServer server = mock(SonarQubeServer.class);
      when(server.id()).thenReturn(id);
      servers.add(server);
    }

    GlobalConfiguration config = mock(GlobalConfiguration.class);
    when(config.servers()).thenReturn(servers);
    return config;
  }

  private ProjectConfiguration createProjectConfig(@Nullable String serverId, @Nullable String projectKey) {
    ProjectConfiguration config = mock(ProjectConfiguration.class);
    when(config.serverId()).thenReturn(serverId);
    when(config.projectKey()).thenReturn(projectKey);
    return config;
  }
}
