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

import org.junit.Before;
import org.junit.Test;
import org.sonarlint.cli.util.MutableInt;

import static org.assertj.core.api.Assertions.assertThat;

public class MutableIntTest {
  private MutableInt integer;
  
  @Before
  public void setUp() {
    integer = new MutableInt();
  }
  
  @Test
  public void inc() {
    assertThat(integer.get()).isEqualTo(0);
    
    integer.inc();
    assertThat(integer.get()).isEqualTo(1);
    
    integer.inc();
    assertThat(integer.get()).isEqualTo(2);
  }
  
  @Test
  public void set() {
    integer.set(10);
    assertThat(integer.get()).isEqualTo(10);
  }
  
  @Test
  public void equals() {
    MutableInt integer1 = new MutableInt(2);
    MutableInt integer2 = new MutableInt(2);
    MutableInt integer3 = new MutableInt(3);
    
    assertThat(integer1).isEqualTo(integer2);
    assertThat(integer1).isEqualTo(integer1);
    assertThat(integer1).isNotEqualTo(integer3);
  }
}
