package org.sonarsource.sonarlint.core.tracking;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIssueTrackerCache implements IssueTrackerCache {

  private final Map<String, Collection<Trackable>> cache = new ConcurrentHashMap<>();

  @Override
  public boolean isFirstAnalysis(String file) {
    return !cache.containsKey(file);
  }

  @Override
  public Collection<Trackable> getCurrentTrackables(String file) {
    return cache.getOrDefault(file, Collections.emptyList());
  }

  @Override
  public Collection<Trackable> getLiveOrFail(String file) {
    Collection<Trackable> trackables = cache.get(file);
    if (trackables == null) {
      throw new IllegalStateException("file should have been already analyzed: " + file);
    }
    return trackables;
  }

  @Override
  public void put(String file, Collection<Trackable> trackables) {
    cache.put(file, trackables);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  public void shutdown() {
    // nothing to do
  }
}
