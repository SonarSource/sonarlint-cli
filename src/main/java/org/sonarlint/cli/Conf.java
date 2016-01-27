/*
 * SonarLint CLI
 * Copyright (C) 2016 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.cli;

import org.sonarlint.cli.util.Logger;
import static org.sonarlint.cli.SonarProperties.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

class Conf {
  private final Properties props;
  private final Logger logger;

  Conf(Properties props) {
    this.props = props;
    this.logger = Logger.get();
  }

  Properties properties() throws IOException {
    Properties result = new Properties();
    initProjectBaseDir(props);
    result.putAll(loadProjectProperties());
    result.putAll(System.getProperties());
    result.putAll(props);
    result.remove(PROJECT_HOME);
    return result;
  }

  private Properties loadProjectProperties() throws IOException {
    File rootSettingsFile = locatePropertiesFile(props, props.containsKey(PROPERTY_PROJECT_BASEDIR) ? PROPERTY_PROJECT_BASEDIR : PROJECT_HOME,
      SONAR_PROJECT_PROPERTIES_FILENAME);
    if (rootSettingsFile != null && rootSettingsFile.isFile() && rootSettingsFile.exists()) {
      logger.info("Project configuration file: " + rootSettingsFile.getAbsolutePath());
      return toProperties(rootSettingsFile);
    }
    logger.info("Project configuration file: NONE");
    return new Properties();
  }

  private static void initProjectBaseDir(Properties props) {
    if (!props.containsKey(PROPERTY_PROJECT_BASEDIR) && props.containsKey(PROJECT_HOME)) {
      String baseDir = props.getProperty(PROJECT_HOME);
      props.put(PROPERTY_PROJECT_BASEDIR, baseDir);
    }
  }

  private static void setProjectBaseDir(File baseDir, Properties childProps, String moduleId) {
    if (!baseDir.isDirectory()) {
      throw new IllegalStateException(MessageFormat.format("The base directory of the module ''{0}'' does not exist: {1}", moduleId, baseDir.getAbsolutePath()));
    }
    childProps.put(PROPERTY_PROJECT_BASEDIR, baseDir.getAbsolutePath());
  }

  private static File locatePropertiesFile(Properties props, String homeKey, String relativePathFromHome) {
    File settingsFile = null;
    String runnerHome = props.getProperty(homeKey, "");
    if (!"".equals(runnerHome)) {
      settingsFile = new File(runnerHome, relativePathFromHome);
    }

    return settingsFile;
  }

  private static Properties toProperties(File file) {
    Properties properties = new Properties();

    try (InputStream in = new FileInputStream(file)) {
      properties.load(in);
      // Trim properties
      for (String propKey : properties.stringPropertyNames()) {
        properties.setProperty(propKey, properties.getProperty(propKey).trim());
      }
      return properties;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to load file: " + file.getAbsolutePath(), e);

    }
  }

  /**
   * @return baseDir
   */
  protected File loadPropsFile(File parentBaseDir, Properties moduleProps, String moduleId) {
    File propertyFile = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_CONFIG_FILE), parentBaseDir);
    if (propertyFile.isFile()) {
      Properties propsFromFile = toProperties(propertyFile);
      for (Entry<Object, Object> entry : propsFromFile.entrySet()) {
        moduleProps.put(entry.getKey(), entry.getValue());
      }
      File baseDir;
      if (moduleProps.containsKey(PROPERTY_PROJECT_BASEDIR)) {
        baseDir = getFileFromPath(moduleProps.getProperty(PROPERTY_PROJECT_BASEDIR), propertyFile.getParentFile());
      } else {
        baseDir = propertyFile.getParentFile();
      }
      setProjectBaseDir(baseDir, moduleProps, moduleId);
      return baseDir;
    } else {
      throw new IllegalStateException("The properties file of the module '" + moduleId + "' does not exist: " + propertyFile.getAbsolutePath());
    }
  }

  /**
   * Returns the file denoted by the given path, may this path be relative to "baseDir" or absolute.
   */
  protected static File getFileFromPath(String path, File baseDir) {
    File propertyFile = new File(path.trim());
    if (!propertyFile.isAbsolute()) {
      propertyFile = new File(baseDir, propertyFile.getPath());
    }
    return propertyFile;
  }

  /**
   * Transforms a comma-separated list String property in to a array of trimmed strings.
   *
   * This works even if they are separated by whitespace characters (space char, EOL, ...)
   *
   */
  static String[] getListFromProperty(Properties properties, String key) {
    String value = properties.getProperty(key, "").trim();
    if (value.isEmpty()) {
      return new String[0];
    }
    String[] values = value.split(",");
    List<String> trimmedValues = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      String trimmedValue = values[i].trim();
      if (!trimmedValue.isEmpty()) {
        trimmedValues.add(trimmedValue);
      }
    }
    return trimmedValues.toArray(new String[trimmedValues.size()]);
  }
}
