/*
 * SonarSource :: IT :: SonarLint CLI
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
package it.sonarlint.cli;

import it.sonarlint.cli.tools.SonarlintCli;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageTest {
  @ClassRule
  public static SonarlintCli sonarlint = new SonarlintCli();

  @Before
  public void setUp() {
    sonarlint.install();
  }

  @Test
  public void testSimpleJava() {
    int code = sonarlint.deployAndRunProject("java-sample");
    assertThat(code).isEqualTo(0);
    
    assertThat(sonarlint.getOut()).contains("6 issues");
    assertThat(sonarlint.getOut()).contains("6 major");
    assertThat(sonarlint.getOut()).contains("2 files analyzed");
  }

  @Test
  public void testMultiLanguage() {
    int code = sonarlint.deployAndRunProject("multi-language", "-X");
    assertThat(code).isEqualTo(0);
    
    assertThat(sonarlint.getOut()).contains("src/main/js/Hello.js' is detected to be 'js'");
    assertThat(sonarlint.getOut()).contains("src/main/java/Hello.java' is detected to be 'java'");

    assertThat(sonarlint.getOut()).contains("3 issues");
    assertThat(sonarlint.getOut()).contains("3 major");
    // 1 of each lang
    assertThat(sonarlint.getOut()).contains("2 files analyzed");
  }
  
  @Test
  public void testNoIssues() {
    int code = sonarlint.deployAndRunProject("java-no-issues");
    assertThat(code).isEqualTo(0);
    
    assertThat(sonarlint.getOut()).contains("No issues to display");
    assertThat(sonarlint.getOut()).contains("1 file analyzed");
  }
  
  @Test
  public void testNoFiles() {
    int code = sonarlint.deployAndRunProject("no-lang-files", "-X");
    assertThat(code).isEqualTo(0);
    
    assertThat(sonarlint.getOut()).contains("No files analyzed");
  }

}
