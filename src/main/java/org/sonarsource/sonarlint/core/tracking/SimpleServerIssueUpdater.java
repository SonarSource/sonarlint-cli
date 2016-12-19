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
package org.sonarsource.sonarlint.core.tracking;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;

public class SimpleServerIssueUpdater {

  private final Logger logger;
  private final Console console;
  private final CachingIssueTracker issueTracker;

  public SimpleServerIssueUpdater(Logger logger, Console console, CachingIssueTracker issueTracker) {
    this.logger = logger;
    this.console = console;
    this.issueTracker = issueTracker;
  }

  public void update(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String moduleKey, Collection<String> fileKeys) {
    try {
      for (String fileKey : fileKeys) {
        List<ServerIssue> serverIssues = fetchServerIssues(serverConfiguration, engine, moduleKey, fileKey);
        Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
        issueTracker.matchAndTrackAsBase(fileKey, serverIssuesTrackable);
      }
    } catch (Exception e) {
      console.error("error while fetching and matching server issues", e);
    }
  }

  private List<ServerIssue> fetchServerIssues(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String moduleKey, String fileKey) {
    try {
      logger.debug("fetchServerIssues moduleKey=" + moduleKey + ", fileKey=" + fileKey);
      return engine.downloadServerIssues(serverConfiguration, moduleKey, fileKey);
    } catch (DownloadException e) {
      logger.debug("failed to download server issues", e);
      console.info(e.getMessage());
      return engine.getServerIssues(moduleKey, fileKey);
    }
  }
}
