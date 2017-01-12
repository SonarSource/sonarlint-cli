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
package org.sonarlint.cli.util;

public class SystemInfo {
  private static System2 system = new System2();

  private SystemInfo() {
  }

  public static void setSystem(System2 system) {
    SystemInfo.system = system;
  }

  public static void print(Logger logger) {
    logger.info(java());
    logger.info(os());
    String runnerOpts = system.getenv("SONARLINT_OPTS");
    if (runnerOpts != null) {
      logger.info("SONARLINT_OPTS=" + runnerOpts);
    }
  }

  public static String getVersion() {
    String version = "unknown";

    Package p = SystemInfo.class.getPackage();
    if (p != null) {
      String implVersion = p.getImplementationVersion();
      if (implVersion != null) {
        version = implVersion;
      }
    }

    return version;
  }

  public static String java() {
    StringBuilder sb = new StringBuilder();
    sb
      .append("Java ")
      .append(system.getProperty("java.version"))
      .append(" ")
      .append(system.getProperty("java.vendor"));
    String bits = system.getProperty("sun.arch.data.model");
    if ("32".equals(bits) || "64".equals(bits)) {
      sb.append(" (").append(bits).append("-bit)");
    }
    return sb.toString();
  }

  public static String os() {
    StringBuilder sb = new StringBuilder();
    sb
      .append(system.getProperty("os.name"))
      .append(" ")
      .append(system.getProperty("os.version"))
      .append(" ")
      .append(system.getProperty("os.arch"));
    return sb.toString();
  }
}
