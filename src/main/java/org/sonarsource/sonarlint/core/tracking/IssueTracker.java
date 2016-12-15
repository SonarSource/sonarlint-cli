package org.sonarsource.sonarlint.core.tracking;

import java.util.Collection;
import java.util.function.BiFunction;

/**
 * Match the next set of issues to the previous set,
 * and carry over content from the previous set.
 *
 * Return the current collection of issues, with content carried over from matched issues
 */
public interface IssueTracker extends BiFunction<Collection<Trackable>, Collection<Trackable>, Collection<Trackable>> {
}
