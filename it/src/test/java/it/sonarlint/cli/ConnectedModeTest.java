/*
 * SonarSource :: IT :: SonarLint CLI
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
package it.sonarlint.cli;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import it.sonarlint.cli.tools.SonarlintCli;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectedModeTest {
  private static final String PROJECT_KEY_JAVA = "sample-java";

  @ClassRule
  public static SonarlintCli sonarlint = new SonarlintCli();

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .restoreProfileAtStartup(FileLocation.ofClasspath("/java-sonarlint.xml"))
    .build();

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @BeforeClass
  public static void prepare() throws Exception {
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY_JAVA, "Sample Java");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY_JAVA, "java", "SonarLint IT Java");
  }

  @Before
  public void setUp() throws IOException {
    createGlobalConfig(ORCHESTRATOR.getServer().getUrl());
    sonarlint.install();
  }

  @Test
  public void testSimpleJava() throws IOException {
    Path projectRoot = sonarlint.deployProject("java-sample");

    createProjectConfig(projectRoot, PROJECT_KEY_JAVA);

    int code = sonarlint.run(projectRoot, "-u");
    assertThat(code).isEqualTo(0);

    assertThat(sonarlint.getOut()).contains("No storage for server");
    assertThat(sonarlint.getOut()).contains("Updating binding");
    assertThat(sonarlint.getOut()).contains("Using storage for server");

    assertThat(sonarlint.getOut()).contains("1 issue");
    assertThat(sonarlint.getOut()).contains("1 major");
    assertThat(sonarlint.getOut()).contains("3 files analyzed");
  }

  private void createProjectConfig(Path projectRoot, String projectKey) throws IOException {
    Path configFile = projectRoot.resolve("sonarlint.json");
    String json = "{serverId=\"local\", projectKey=" + projectKey + "}";
    Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
  }

  private void createGlobalConfig(String serverUrl) throws IOException {
    Path configDir = temp.getRoot().toPath().resolve(".sonarlint").resolve("conf");
    Files.createDirectories(configDir);
    Path configFile = configDir.resolve("global.json");
    String json = "{servers=[{id=\"local\",url=\"" + serverUrl + "\"}]}";
    Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
    sonarlint.addEnv("SONARLINT_OPTS", "-Duser.home=" + temp.getRoot().toPath().toAbsolutePath().toString());
  }

}
