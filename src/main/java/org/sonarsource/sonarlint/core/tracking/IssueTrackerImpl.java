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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class IssueTrackerImpl implements IssueTracker {

  @Override
  public Collection<Trackable> apply(Collection<Trackable> baseIssues, Collection<Trackable> nextIssues) {
    Collection<Trackable> trackedIssues = new ArrayList<>();
    Tracking<Trackable, Trackable> tracking = new Tracker<Trackable, Trackable>().track(() -> nextIssues, () -> baseIssues);
    for (Map.Entry<Trackable, Trackable> entry : tracking.getMatchedRaws().entrySet()) {
      Trackable next = new CombinedTrackable(entry.getValue(), entry.getKey());
      trackedIssues.add(next);
    }
    for (Trackable next : tracking.getUnmatchedRaws()) {
      if (next.getServerIssueKey() != null) {
        next = new DisconnectedTrackable(next);
      } else if (next.getCreationDate() == null) {
        next = new LeakedTrackable(next);
      }
      trackedIssues.add(next);
    }
    return trackedIssues;
  }
}
