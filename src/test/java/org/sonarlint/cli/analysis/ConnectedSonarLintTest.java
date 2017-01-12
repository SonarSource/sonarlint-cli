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
package org.sonarlint.cli.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarlint.cli.config.SonarQubeServer;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ModuleStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.tracking.Trackable;

public class ConnectedSonarLintTest {
  private ConnectedSonarLintEngine engine;
  private ConnectedSonarLint sonarLint;

  private static AtomicInteger counter = new AtomicInteger();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    SonarQubeServer server = mock(SonarQubeServer.class);
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
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(true);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    when(engine.getGlobalStorageStatus()).thenReturn(status);
    sonarLint.start(false);

    verify(engine).update(any(ServerConfiguration.class));
    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
  }

  @Test
  public void testModuleUpdateOnly() {
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    when(engine.getGlobalStorageStatus()).thenReturn(status);
    sonarLint.start(false);

    verify(engine).updateModule(any(ServerConfiguration.class), eq("project1"));
    verify(engine).allModulesByKey();
    verify(engine).getGlobalStorageStatus();
    verify(engine).getModuleStorageStatus("project1");
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void testModuleDoesntExistInUpdate() {
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(true);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("p"));
    when(engine.getGlobalStorageStatus()).thenReturn(status);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Project key 'project1' not found in the SonarQube server");
    sonarLint.start(false);

  }

  @Test
  public void testModuleDoesntExist() {
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.allModulesByKey()).thenReturn(getModulesByKey("p"));
    when(engine.getGlobalStorageStatus()).thenReturn(status);

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
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.getGlobalStorageStatus()).thenReturn(status);

    ModuleStorageStatus moduleStatus = mock(ModuleStorageStatus.class);
    when(engine.getModuleStorageStatus("project1")).thenReturn(moduleStatus);

    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    sonarLint.start(false);

    verify(engine).allModulesByKey();
    verify(engine).getGlobalStorageStatus();
    verify(engine).getModuleStorageStatus("project1");
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void testModuleStorageUpdateNeeded() {
    GlobalStorageStatus status = mock(GlobalStorageStatus.class);
    when(status.isStale()).thenReturn(false);
    when(engine.getGlobalStorageStatus()).thenReturn(status);

    ModuleStorageStatus moduleStatus = mock(ModuleStorageStatus.class);
    when(moduleStatus.isStale()).thenReturn(true);
    String moduleKey = "project1";
    when(engine.getModuleStorageStatus(moduleKey)).thenReturn(moduleStatus);

    when(engine.allModulesByKey()).thenReturn(getModulesByKey(moduleKey));
    sonarLint.start(false);

    verify(engine).allModulesByKey();
    verify(engine).getGlobalStorageStatus();
    verify(engine).getModuleStorageStatus(moduleKey);
    verify(engine).updateModule(any(), eq(moduleKey));
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void should_use_token_authentication_when_available() {
    SonarQubeServer server = mock(SonarQubeServer.class);
    when(server.url()).thenReturn("http://localhost:9000");
    when(server.token()).thenReturn("dummy token");
    engine = mock(ConnectedSonarLintEngine.class);
    sonarLint = new ConnectedSonarLint(engine, server, "project1");

    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    sonarLint.start(false);

    // 2 calls: 1 to update global data and 1 to update module data
    verify(server, times(2)).url();
    verify(server, times(2)).token();
    verifyNoMoreInteractions(server);
  }

  @Test
  public void should_use_login_and_password_when_token_null() {
    SonarQubeServer server = mock(SonarQubeServer.class);
    when(server.url()).thenReturn("http://localhost:9000");
    engine = mock(ConnectedSonarLintEngine.class);
    sonarLint = new ConnectedSonarLint(engine, server, "project1");

    when(engine.allModulesByKey()).thenReturn(getModulesByKey("project1"));
    sonarLint.start(false);

    // 2 calls: 1 to update global data and 1 to update module data
    verify(server, times(2)).url();
    verify(server, times(2)).token();
    verify(server, times(2)).login();
    verify(server, times(2)).password();
    verifyNoMoreInteractions(server);
  }

  private Map<String, RemoteModule> getModulesByKey(String... keys) {
    Map<String, RemoteModule> map = new HashMap<>();
    for (String k : keys) {
      RemoteModule module = mock(RemoteModule.class);
      map.put(k, module);
    }
    return map;
  }

  @Test
  public void test_getRelativePath() {
    Path moduleRoot = Paths.get("").toAbsolutePath();
    String relativePath = Paths.get("some").resolve("relative").resolve("path").toString();

    Issue issue = mockIssue();
    when(issue.getInputFile().getPath()).thenReturn(moduleRoot.resolve(relativePath).toString());
    assertThat(sonarLint.getRelativePath(moduleRoot, issue)).isEqualTo(relativePath);
  }

  @Test
  public void getRelativePath_should_return_null_when_clientInputFile_is_null() {
    assertThat(sonarLint.getRelativePath(Paths.get("dummy"), mock(Issue.class))).isNull();
  }

  @Test
  public void should_not_match_server_issues_when_there_are_none() {
    Path moduleRoot = Paths.get("").toAbsolutePath();

    Issue issue = mockIssue();
    when(issue.getInputFile().getPath()).thenReturn(moduleRoot.resolve("dummy").toString());

    Collection<Issue> issues = Collections.singletonList(issue);
    Collection<Trackable> trackables = sonarLint.matchAndTrack(moduleRoot, issues);
    assertThat(trackables).extracting("issue").isEqualTo(issues);
  }

  @Test
  public void should_hide_resolved_server_issues() {
    Path moduleRoot = Paths.get("").toAbsolutePath();
    String dummyFilePath = moduleRoot.resolve("dummy").toString();

    Issue unresolved = mockIssue();
    when(unresolved.getInputFile().getPath()).thenReturn(dummyFilePath);
    Issue resolved = mockIssue();
    when(resolved.getInputFile().getPath()).thenReturn(dummyFilePath);

    Collection<Issue> issues = Arrays.asList(unresolved, resolved);
    ServerIssue resolvedServerIssue = mockServerIssue(resolved);
    List<ServerIssue> serverIssues = Arrays.asList(mockServerIssue(unresolved), resolvedServerIssue);
    when(engine.getServerIssues(any(), any())).thenReturn(serverIssues);

    Collection<Trackable> trackables = sonarLint.matchAndTrack(moduleRoot, issues);
    assertThat(trackables).extracting("issue").containsOnlyElementsOf(issues);

    when(resolvedServerIssue.resolution()).thenReturn("CLOSED");
    Collection<Trackable> trackables2 = sonarLint.matchAndTrack(moduleRoot, issues);
    assertThat(trackables2).extracting("issue").isEqualTo(Collections.singletonList(unresolved));
  }

  @Test
  public void should_get_creation_date_from_matched_server_issue() {
    Path moduleRoot = Paths.get("").toAbsolutePath();
    String dummyFilePath = moduleRoot.resolve("dummy").toString();

    Issue unmatched = mockIssue();
    when(unmatched.getInputFile().getPath()).thenReturn(dummyFilePath);
    Issue matched = mockIssue();
    when(matched.getInputFile().getPath()).thenReturn(dummyFilePath);

    Collection<Issue> issues = Arrays.asList(unmatched, matched);
    ServerIssue matchedServerIssue = mockServerIssue(matched);
    List<ServerIssue> serverIssues = Arrays.asList(mockServerIssue(mockIssue()), matchedServerIssue);
    when(engine.getServerIssues(any(), any())).thenReturn(serverIssues);

    Collection<Trackable> trackables = sonarLint.matchAndTrack(moduleRoot, issues);
    assertThat(trackables).extracting("ruleKey").containsOnly(unmatched.getRuleKey(), matched.getRuleKey());
    assertThat(trackables.stream().filter(t -> t.getRuleKey().equals(matched.getRuleKey())).collect(Collectors.toList()))
      .extracting("ruleKey", "creationDate").containsOnly(
      Tuple.tuple(matched.getRuleKey(), matchedServerIssue.creationDate().toEpochMilli())
    );
  }

  @Test
  public void should_create_reports_for_empty_analysis() throws IOException {
    ReportFactory reportFactory = mock(ReportFactory.class);
    Path baseDirPath = Paths.get("nonexistent");
    sonarLint.doAnalysis(Collections.emptyMap(), reportFactory, Collections.emptyList(), baseDirPath);
    verify(reportFactory).createReporters(baseDirPath);
  }

  @Test
  public void test_getRuleDetails() {
    String ruleKey = "dummy key";
    RuleDetails ruleDetails = mock(RuleDetails.class);
    when(engine.getRuleDetails(ruleKey)).thenReturn(ruleDetails);
    assertThat(sonarLint.getRuleDetails(ruleKey)).isEqualTo(ruleDetails);
  }

  // create uniquely identifiable issue
  private Issue mockIssue() {
    Issue issue = mock(Issue.class);

    // basic setup to prevent NPEs
    when(issue.getInputFile()).thenReturn(mock(ClientInputFile.class));
    when(issue.getMessage()).thenReturn("dummy message " + counter.incrementAndGet());

    // make issue match by rule key + line + text range hash
    when(issue.getRuleKey()).thenReturn("dummy ruleKey" + counter.incrementAndGet());
    when(issue.getStartLine()).thenReturn(counter.incrementAndGet());
    return issue;
  }

  // copy enough fields so that tracker finds a match
  private ServerIssue mockServerIssue(Issue issue) {
    ServerIssue serverIssue = mock(ServerIssue.class);

    // basic setup to prevent NPEs
    when(serverIssue.creationDate()).thenReturn(Instant.ofEpochMilli(counter.incrementAndGet()));
    when(serverIssue.resolution()).thenReturn("");
    when(serverIssue.checksum()).thenReturn("dummy checksum " + counter.incrementAndGet());

    // if issue itself is a mock, need to extract value to variable first
    // as Mockito doesn't handle nested mocking inside mocking

    String message = issue.getMessage();
    when(serverIssue.message()).thenReturn(message);

    // copy fields to match during tracking

    String ruleKey = issue.getRuleKey();
    when(serverIssue.ruleKey()).thenReturn(ruleKey);

    Integer startLine = issue.getStartLine();
    when(serverIssue.line()).thenReturn(startLine);

    return serverIssue;
  }
}
