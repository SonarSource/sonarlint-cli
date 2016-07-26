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
package org.sonarlint.cli.analysis;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarlint.cli.config.SonarQubeServer;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalUpdateStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ModuleUpdateStatus;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;

public class ConnectedSonarLintTest {
  private ConnectedSonarLintEngine engine;
  private ConnectedSonarLint sonarLint;
  private SonarQubeServer server;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    server = mock(SonarQubeServer.class);
    when(server.url()).thenReturn("http://localhost:9000");
    engine = mock(ConnectedSonarLintEngine.class);
    sonarLint = new ConnectedSonarLint(engine, server, "project1");
  }

  @Test
  public void testForceUpdate() {
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    sonarLint.start(true);

    verify(engine).update(any(ServerConfiguration.class));
    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
  }

  @Test
  public void testNoUpdate() {
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    sonarLint.start(false);

    verify(engine).update(any(ServerConfiguration.class));
    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
  }

  @Test
  public void testStaleUpdate() {
    GlobalUpdateStatus status = mock(GlobalUpdateStatus.class);
    when(status.isStale()).thenReturn(true);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    when(engine.getUpdateStatus()).thenReturn(status);
    sonarLint.start(false);

    verify(engine).update(any(ServerConfiguration.class));
    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
  }

  @Test
  public void testModuleUpdateOnly() {
    GlobalUpdateStatus status = mock(GlobalUpdateStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    when(engine.getUpdateStatus()).thenReturn(status);
    sonarLint.start(false);

    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
    verify(engine).allModulesByKey();
    verify(engine).getUpdateStatus();
    verify(engine).getModuleUpdateStatus("project1");
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void testModuleDoesntExistInUpdate() {
    GlobalUpdateStatus status = mock(GlobalUpdateStatus.class);
    when(status.isStale()).thenReturn(true);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("p"));
    when(engine.getUpdateStatus()).thenReturn(status);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Project key 'project1' not found in the SonarQube server");
    sonarLint.start(false);

  }

  @Test
  public void testModuleDoesntExist() {
    GlobalUpdateStatus status = mock(GlobalUpdateStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("p"));
    when(engine.getUpdateStatus()).thenReturn(status);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Project key 'project1' not found in the binding storage");
    sonarLint.start(false);
  }

  @Test
  public void testStop() {
    sonarLint.stop();
    verify(engine).stop(false);
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void testUpdateNotNeeded() {
    GlobalUpdateStatus status = mock(GlobalUpdateStatus.class);
    ModuleUpdateStatus moduleStatus = mock(ModuleUpdateStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.getModuleUpdateStatus("project1")).thenReturn(moduleStatus);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    when(engine.getUpdateStatus()).thenReturn(status);
    sonarLint.start(false);

    verify(engine).allModulesByKey();
    verify(engine).getUpdateStatus();
    verify(engine).getModuleUpdateStatus("project1");
    verifyNoMoreInteractions(engine);
  }

  private Map<String, RemoteModule> getModulesByKey(String... keys) {
    Map<String, RemoteModule> map = new HashMap<>();
    for (String k : keys) {
      RemoteModule module = mock(RemoteModule.class);
      map.put(k, module);
    }
    return map;
  }
}
