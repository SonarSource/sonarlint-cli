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
package org.sonarlint.cli;

import java.text.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionsTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testHelp() throws ParseException {
    Options opt = Options.parse(args("-h"));
    assertThat(opt.isHelp()).isTrue();

    opt = Options.parse(args("--help"));
    assertThat(opt.isHelp()).isTrue();
  }

  @Test
  public void testReport() throws ParseException {
    Options opt = Options.parse(args("--html-report", "myreport"));

    assertThat(opt.htmlReport()).isEqualTo("myreport");
  }

  @Test
  public void testGlobs() throws ParseException {
    Options opt = Options.parse(args("--src", "source", "--tests", "tests", "--exclude", "exclude"));

    assertThat(opt.src()).isEqualTo("source");
    assertThat(opt.tests()).isEqualTo("tests");
    assertThat(opt.exclusions()).isEqualTo("exclude");
  }

  @Test
  public void testStack() throws ParseException {
    Options opt = Options.parse(args("-e"));
    assertThat(opt.showStack()).isTrue();

    opt = Options.parse(args("--errors"));
    assertThat(opt.showStack()).isTrue();
  }

  @Test
  public void testCharset() throws ParseException {
    Options opt = Options.parse(args("--charset", "UTF-8"));
    assertThat(opt.charset()).isEqualTo("UTF-8");
  }

  @Test
  public void testUpdate() throws ParseException {
    Options opt = Options.parse(args("-u"));
    assertThat(opt.isUpdate()).isTrue();

    opt = Options.parse(args("--update"));
    assertThat(opt.isUpdate()).isTrue();
  }

  @Test
  public void testInteractive() throws ParseException {
    Options opt = Options.parse(args("-i"));
    assertThat(opt.isInteractive()).isTrue();

    opt = Options.parse(args("--interactive"));
    assertThat(opt.isInteractive()).isTrue();
  }

  @Test
  public void testVersion() throws ParseException {
    Options opt = Options.parse(args("-v"));
    assertThat(opt.isVersion()).isTrue();

    opt = Options.parse(args("--version"));
    assertThat(opt.isVersion()).isTrue();
  }

  @Test
  public void testVerbose() throws ParseException {
    Options opt = Options.parse(args("-X"));
    assertThat(opt.isVerbose()).isTrue();
  }

  @Test
  public void testTask() throws ParseException {
    Options opt = Options.parse(args("mytask"));
    assertThat(opt.task()).isEqualTo("mytask");
  }

  @Test
  public void testInvalidArg() throws ParseException {
    exception.expect(ParseException.class);
    exception.expectMessage("Unrecognized option:");
    Options.parse(args("-a"));
  }

  @Test
  public void testArgMissing() throws ParseException {
    exception.expect(ParseException.class);
    exception.expectMessage("Missing argument for option -D");
    Options.parse(args("-D"));
  }

  @Test
  public void testProperties() throws ParseException {
    Options opt = Options.parse(args("-Dkey=value", "--define", "key2=value2"));

    assertThat(opt.properties()).containsEntry("key", "value");
    assertThat(opt.properties()).containsEntry("key2", "value2");
  }

  @Test
  public void testCombinedOptions() throws ParseException {
    Options opt = Options.parse(args("-X", "-e", "--help"));

    assertThat(opt.isVerbose()).isTrue();
    assertThat(opt.showStack()).isTrue();
    assertThat(opt.isHelp()).isTrue();
  }

  private static String[] args(String... str) {
    return str;
  }
}
