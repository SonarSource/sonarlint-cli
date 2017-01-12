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
package org.sonarlint.cli.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationReaderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void readProjectConfig() throws IOException {
    String json = "{serverId=\"localhost\",projectKey=project1}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    ProjectConfiguration projectConfig = new ConfigurationReader().readProject(file);

    assertThat(projectConfig.serverId()).isEqualTo("localhost");
    assertThat(projectConfig.projectKey()).isEqualTo("project1");
  }

  @Test
  public void readEmptyFileProject() throws IOException {
    String json = "";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to parse JSON file");
    new ConfigurationReader().readProject(file);
  }

  @Test
  public void readEmptyFileGlobal() throws IOException {
    String json = "";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to parse JSON file");
    new ConfigurationReader().readGlobal(file);
  }

  @Test
  public void readInvalidJsonGlobal() throws IOException {
    String json = "{"
      + "servers = ["
      + " { ] {]}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to parse JSON file");
    new ConfigurationReader().readGlobal(file);
  }

  @Test
  public void readInvalidJsonProject() throws IOException {
    String json = "{"
      + "servers = ["
      + " { ] {]}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Failed to parse JSON file");
    new ConfigurationReader().readProject(file);
  }

  @Test
  public void readGlobalConfig() throws IOException {
    String json = "{"
      + "servers = [{"
      + "  url = \"http://localhost:9000\","
      + "  token = mytoken"
      + "}"
      + "]}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    GlobalConfiguration config = new ConfigurationReader().readGlobal(file);

    assertThat(config.servers()).hasSize(1);
    assertThat(config.servers().get(0).url()).isEqualTo("http://localhost:9000");
    assertThat(config.servers().get(0).token()).isEqualTo("mytoken");
  }

  @Test
  public void failWithRepeatedServers() throws IOException {
    String json = "{"
      + "servers = ["
      + "  {url = \"http://localhost:9000\",token = mytoken},"
      + "  {url = \"http://localhost:9000\",token = mytoken}"
      + "]}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Invalid SonarQube servers configuration: Each server configured must have a unique URL");
    new ConfigurationReader().readGlobal(file);
  }

  @Test
  public void failServerWithoutUrl() throws IOException {
    String json = "{"
      + "servers = ["
      + "  {token = mytoken}"
      + "]}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Invalid SonarQube servers configuration: server URL must be defined");
    new ConfigurationReader().readGlobal(file);
  }

  @Test
  public void failProjectWithoutBindingKey() throws IOException {
    String json = "{serverUrl=\"http://localhost:9000\"}";

    Path file = temp.newFile().toPath();
    Files.write(file, json.getBytes(StandardCharsets.UTF_8));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Project binding must have a project key defined");
    new ConfigurationReader().readProject(file);
  }
}
