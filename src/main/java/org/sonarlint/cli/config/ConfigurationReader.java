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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

public class ConfigurationReader {
  private static final String FAIL_PARSE_JSON = "Failed to parse JSON file: ";

  public GlobalConfiguration readGlobal(Path configFilePath) {
    String contents = getContents(configFilePath);
    GlobalConfiguration config;

    try {
      config = new GsonBuilder().create().fromJson(contents, GlobalConfiguration.class);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(FAIL_PARSE_JSON + configFilePath, ex);
    }

    return validate(config, configFilePath);
  }

  public ProjectConfiguration readProject(Path configFilePath) {
    String contents = getContents(configFilePath);
    ProjectConfiguration config;

    try {
      config = new GsonBuilder().create().fromJson(contents, ProjectConfiguration.class);
    } catch (RuntimeException ex) {
      throw new IllegalStateException(FAIL_PARSE_JSON + configFilePath, ex);
    }

    return validate(config, configFilePath);
  }

  private static String getContents(Path filePath) {
    try {
      return Files.toString(filePath.toFile(), Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Error reading configuration file: " + filePath.toAbsolutePath(), e);
    }
  }

  private static ProjectConfiguration validate(@Nullable ProjectConfiguration projectConfig, Path configFilePath) {
    if (projectConfig == null) {
      throw new IllegalStateException(FAIL_PARSE_JSON + configFilePath.toAbsolutePath());
    }
    if (StringUtils.isEmpty(projectConfig.projectKey())) {
      throw new IllegalStateException("Project binding must have a project key defined. Check the configuration in: " + configFilePath.toAbsolutePath());
    }
    return projectConfig;
  }

  private static GlobalConfiguration validate(@Nullable GlobalConfiguration globalConfig, Path configFilePath) {
    Set<String> serverUrls = new HashSet<>();

    if (globalConfig == null) {
      throw new IllegalStateException(FAIL_PARSE_JSON + configFilePath.toAbsolutePath());
    }
    if (globalConfig.servers() != null) {
      for (SonarQubeServer s : globalConfig.servers()) {
        if (StringUtils.isEmpty(s.url())) {
          throw new IllegalStateException("Invalid SonarQube servers configuration: server URL must be defined. Check the configuration in: " + configFilePath);
        }

        if (serverUrls.contains(s.url())) {
          throw new IllegalStateException("Invalid SonarQube servers configuration: Each server configured must have a unique URL. Check the configuration in: " + configFilePath);
        }

        serverUrls.add(s.url());
      }
    }
    return globalConfig;
  }
}
