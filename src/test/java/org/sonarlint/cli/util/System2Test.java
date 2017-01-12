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

import org.assertj.core.data.Percentage;
import org.junit.Test;

import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.assertThat;

public class System2Test {
  @Test
  public void testProxy() {
    System.setProperty("test1", "prop1");
    
    assertThat(System2.INSTANCE.envVariables()).isEqualTo(System.getenv());
    assertThat(System2.INSTANCE.property("test1")).isEqualTo("prop1");
    assertThat(System2.INSTANCE.getProperty("test1")).isEqualTo("prop1");
    assertThat(System2.INSTANCE.properties()).isEqualTo(System.getProperties());
    
    Entry<String, String> envVar = System.getenv().entrySet().iterator().next();
    
    assertThat(System2.INSTANCE.envVariable(envVar.getKey())).isEqualTo(envVar.getValue());
    assertThat(System2.INSTANCE.getenv(envVar.getKey())).isEqualTo(envVar.getValue());
    
    assertThat(System2.INSTANCE.now()).isCloseTo(System.currentTimeMillis(), Percentage.withPercentage(0.01));
  }
}
