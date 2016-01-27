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

import java.text.ParseException;
import java.util.Properties;

public class Options {
  private Properties props = new Properties();
  private boolean verbose = false;
  private boolean help = false;
  private boolean version = false;
  private boolean showStack = false;
  private boolean interactive = false;
  private String reportDir = null;
  private String reportName = null;
  private String task;

  public static Options parse(String[] args) throws ParseException {
    Options options = new Options();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (i == 0 && !arg.startsWith("-")) {
        options.task = arg;

      } else if ("-h".equals(arg) || "--help".equals(arg)) {
        options.help = true;

      } else if ("-v".equals(arg) || "--version".equals(arg)) {
        options.version = true;

      } else if ("-i".equals(arg) || "--interactive".equals(arg)) {
        options.interactive = true;

      } else if ("-e".equals(arg) || "--errors".equals(arg)) {
        options.showStack = true;

      } else if ("-X".equals(arg) || "--debug".equals(arg)) {
        options.verbose = true;

      } else if ("--report-dir".equals(arg)) {
        i++;
        if (i >= args.length) {
          throw new ParseException("Missing argument for option --report-dir", i);
        }
        options.reportDir = args[i];

      } else if ("--report-name".equals(arg)) {
        i++;
        if (i >= args.length) {
          throw new ParseException("Missing argument for option --report-name", i);
        }
        options.reportName = args[i];

      } else if ("-D".equals(arg) || "--define".equals(arg)) {
        i++;
        if (i >= args.length) {
          throw new ParseException("Missing argument for option --define", i);
        }
        arg = args[i];
        appendPropertyTo(arg, options.props);

      } else if (arg.startsWith("-D")) {
        arg = arg.substring(2);
        appendPropertyTo(arg, options.props);

      } else {
        throw new ParseException("Unrecognized option: " + arg, i);
      }
    }

    return options;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public boolean isInteractive() {
    return interactive;
  }

  public boolean isHelp() {
    return help;
  }

  public String reportName() {
    return reportName;
  }

  public String reportDir() {
    return reportDir;
  }

  public boolean isVersion() {
    return version;
  }

  public boolean showStack() {
    return showStack;
  }

  public Properties properties() {
    return props;
  }

  public String task() {
    return task;
  }

  public static void printUsage() {
    Logger logger = Logger.get();
    logger.info("");
    logger.info("usage: sonarlint [options]");
    logger.info("");
    logger.info("Options:");
    logger.info(" -D,--define <arg>     Define property");
    logger.info(" -e,--errors           Produce execution error messages");
    logger.info(" -h,--help             Display help information");
    logger.info(" -v,--version          Display version information");
    logger.info(" -X,--debug            Produce execution debug output");
    logger.info(" -i,--interactive      Run interactively");
    logger.info(" --report-dir <dir>    Report output directory");
    logger.info(" --report-name <name>  Report output name");
  }

  private static void appendPropertyTo(String arg, Properties props) {
    final String key, value;
    int j = arg.indexOf('=');
    if (j == -1) {
      key = arg;
      value = "true";
    } else {
      key = arg.substring(0, j);
      value = arg.substring(j + 1);
    }
    props.setProperty(key, value);
  }
}
