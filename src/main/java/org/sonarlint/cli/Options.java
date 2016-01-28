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
  private String htmlReport = null;
  private String src = null;
  private String tests = null;
  private String charset = null;
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

      } else if (arg.startsWith("-D") && !"-D".equals(arg)) {
        arg = arg.substring(2);
        appendPropertyTo(arg, options.props);

      } else {
        i++;

        if ("--html-report".equals(arg)) {
          checkAdditionalArg(i, args.length, arg);
          options.htmlReport = args[i];

        } else if ("--charset".equals(arg)) {
          checkAdditionalArg(i, args.length, arg);
          options.charset = args[i];

        } else if ("--src".equals(arg)) {
          checkAdditionalArg(i, args.length, arg);
          options.src = args[i];

        } else if ("--tests".equals(arg)) {
          checkAdditionalArg(i, args.length, arg);
          options.tests = args[i];

        } else if ("-D".equals(arg) || "--define".equals(arg)) {
          checkAdditionalArg(i, args.length, arg);
          appendPropertyTo(args[i], options.props);

        } else {
          throw new ParseException("Unrecognized option: " + arg, i);
        }
      }
    }

    return options;
  }

  private static void checkAdditionalArg(int i, int argsLength, String arg) throws ParseException {
    if (i >= argsLength) {
      throw new ParseException("Missing argument for option " + arg, i);
    }
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

  public String charset() {
    return charset;
  }

  public String htmlReport() {
    return htmlReport;
  }

  public String src() {
    return src;
  }

  public String tests() {
    return tests;
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
    logger.info(" -D,--define <arg>      Define property");
    logger.info(" -e,--errors            Produce execution error messages");
    logger.info(" -h,--help              Display help information");
    logger.info(" -v,--version           Display version information");
    logger.info(" -X,--debug             Produce execution debug output");
    logger.info(" -i,--interactive       Run interactively");
    logger.info(" --html-report <path>   HTML report output path (relative or absolute)");
    logger.info(" --src <glob pattern>   GLOB pattern to identify source files");
    logger.info(" --tests <glob pattern> GLOB pattern to identify test files");
    logger.info(" --charset <name>       Character encoding of the source files");
  }

  private static void appendPropertyTo(String arg, Properties props) {
    final String key;
    final String value;

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
