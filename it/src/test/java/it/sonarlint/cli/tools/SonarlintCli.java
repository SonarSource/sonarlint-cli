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
package it.sonarlint.cli.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SonarlintCli extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(SonarlintCli.class);
  private Path script;
  private Path installPath;
  private Path project;
  private CommandExecutor exec;
  private Map<String, String> env = new HashMap<>();

  public void install() {
    String version = System.getProperty("sonarlint.version");
    if (version == null) {
      throw new IllegalStateException("No sonarlint-cli version specified. Use '-Dsonarlint.version'.");
    }
    install(version);
  }

  public void install(String version) {
    SonarlintInstaller installer = new SonarlintInstaller();
    this.script = installer.install(installPath, version);
  }
  
  public void addEnv(String key, String value) {
    env.put(key, value);
  }

  public int run(Path workingDir, String... args) {
    LOG.info("Running SonarLint CLI in '{}'", workingDir.toAbsolutePath());
    try {
      exec = new CommandExecutor(script);
      return exec.execute(args, workingDir.toAbsolutePath(), env);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
  
  public int deployAndRunProject(String location, String... args) {
    LOG.info("Running SonarLint CLI on project '{}'", location);
    try {
      Path project = deployProject(location);
      exec = new CommandExecutor(script);
      return exec.execute(args, project, env);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
  
  public Path deployProject(String location) throws IOException {
    Path originalLoc = Paths.get("projects").resolve(location);
    String projectName = originalLoc.getFileName().toString();

    if (!Files.isDirectory(originalLoc)) {
      throw new IllegalArgumentException("Couldn't find project directory: " + originalLoc.toAbsolutePath().toString());
    }

    cleanProject();
    project = Files.createTempDirectory(projectName);
    FileUtils.copyDirectory(originalLoc.toFile(), project.toFile());
    return project;
  }

  private void cleanProject() {
    if (project != null) {
      FileUtils.deleteQuietly(project.toFile());
      project = null;
    }
  }

  protected void before() throws Throwable {
    installPath = Files.createTempDirectory("sonarlint-cli");
  }

  protected void after() {
    cleanProject();
    if (installPath != null) {
      FileUtils.deleteQuietly(installPath.toFile());
      installPath = null;
    }
  }

  public String getOut() {
    return exec.getStdOut();
  }

  public String[] getOutLines() {
    return getOut().split(System.lineSeparator());
  }

  public Path getProject() {
    return project;
  }

  public Path getSonarlintInstallation() {
    return script.getParent().getParent();
  }

  public String getErr() {
    return exec.getStdErr();
  }

  public String[] getErrLines() {
    return getErr().split(System.lineSeparator());
  }
}
