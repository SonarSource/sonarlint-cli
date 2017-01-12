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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

public class Util {
  private Util() {
    // static only
  }

  public static <T, U> U getOrCreate(Map<T, U> map, T key, Supplier<U> f) {
    U value = map.get(key);
    if (value != null) {
      return value;
    }
    value = f.get();
    map.put(key, value);
    return value;
  }
  
  public static Map<String, String> toMap(Properties properties) {
    return new HashMap<>((Map) properties);
  }
  
  public static String escapeFileName(String fileName) {
    return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
  }
}
