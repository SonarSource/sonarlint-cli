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

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.*;

public class ConfTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Properties args = new Properties();
  Logger logs = Logger.get();
  Conf conf = new Conf(args);

  @Test
  public void should_load_global_settings_by_home() throws Exception {
    File home = getTestFile("shouldLoadRunnerSettingsByHome/");
    args.setProperty("sonarlint.home", home.getCanonicalPath());

    assertThat(conf.properties().get("sonar.prop")).isEqualTo("value");
  }

  @Test
  public void should_not_fail_if_no_home() throws Exception {
    assertThat(conf.properties()).isNotEmpty();
  }

  @Test
  public void should_load_conf_by_direct_path() throws Exception {
    File settings = getTestFile("shouldLoadRunnerSettingsByDirectPath/other-conf.properties");
    args.setProperty("runner.settings", settings.getCanonicalPath());

    assertThat(conf.properties().get("sonar.prop")).isEqualTo("otherValue");
  }

  @Test
  public void shouldLoadCompleteConfiguration() throws Exception {
    File sonarlintHome = getTestFile("shouldLoadCompleteConfiguration/runner");
    File projectHome = getTestFile("shouldLoadCompleteConfiguration/project");
    args.setProperty("sonarlint.home", sonarlintHome.getCanonicalPath());
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("project.prop")).isEqualTo("foo");
    assertThat(properties.getProperty("overridden.prop")).isEqualTo("project scope");
    assertThat(properties.getProperty("global.prop")).isEqualTo("jdbc:mysql:localhost/sonar");
  }

  @Test
  public void shouldLoadModuleConfiguration() throws Exception {
    File projectHome = getTestFile("shouldLoadModuleConfiguration/project");
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
  }

  @Test
  public void shouldSupportDeepModuleConfigurationInRoot() throws Exception {
    File projectHome = getTestFile("shouldSupportDeepModuleConfigurationInRoot/project");
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("1.11.sonar.projectName")).isEqualTo("Module 11");
    assertThat(properties.getProperty("1.11.111.sonar.projectName")).isEqualTo("Module 111");
    assertThat(properties.getProperty("1.12.sonar.projectName")).isEqualTo("Module 12");
    assertThat(properties.getProperty("2.sonar.projectName")).isEqualTo("Module 2");

    // SONARUNNER-125
    assertThat(properties.getProperty("11.111.sonar.projectName")).isNull();
  }

  @Test
  public void shouldLoadModuleConfigurationOverrideBasedir() throws Exception {
    File projectHome = getTestFile("shouldLoadModuleConfigurationOverrideBasedir/project");
    args.setProperty("project.home", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
    assertThat(properties.getProperty("module3.sonar.projectName")).isEqualTo("Module 3");
  }

  @Test
  public void shouldSupportSettingBaseDirFromCli() throws Exception {
    File projectHome = getTestFile("shouldLoadModuleConfiguration/project");
    args.setProperty("project.home", temp.newFolder().getCanonicalPath());
    args.setProperty("sonar.projectBaseDir", projectHome.getCanonicalPath());

    Properties properties = conf.properties();

    assertThat(properties.getProperty("module1.sonar.projectName")).isEqualTo("Module 1");
    assertThat(properties.getProperty("module2.sonar.projectName")).isEqualTo("Module 2");
  }

  @Test
  public void ignoreEmptyModule() throws Exception {
    File projectHome = getTestFile("emptyModules/project");
    args.setProperty("project.home", temp.newFolder().getCanonicalPath());
    args.setProperty("sonar.projectBaseDir", projectHome.getCanonicalPath());

    conf.properties();
  }

  @Test
  public void shouldGetList() {
    Properties props = new Properties();

    props.put("prop", "  foo  ,,  bar  , \n\ntoto,tutu");
    assertThat(Conf.getListFromProperty(props, "prop")).containsOnly("foo", "bar", "toto", "tutu");
  }

  private File getTestFile(String name) throws URISyntaxException {
    return new File(getClass().getResource("/ConfTest/" + name).toURI());
  }

}
