package org.sonarsource.sonarlint.core.tracking;

import java.util.Collection;

public interface CachingIssueTracker extends IssueTracker {
  /**
   * Match a new set of trackables to current state.
   *
   * @param file the file analyzed
   * @param trackables the trackables in the file
   */
  Collection<Trackable> matchAndTrackAsNew(String file, Collection<Trackable> trackables);

  /**
   * "Rebase" current trackables against given trackables.
   *
   * @param file the file analyzed
   * @param trackables the trackables in the file
   */
  Collection<Trackable> matchAndTrackAsBase(String file, Collection<Trackable> trackables);
}
