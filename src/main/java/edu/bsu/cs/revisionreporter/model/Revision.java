
package edu.bsu.cs.revisionreporter.model;

import java.time.Instant;

/** Immutable wiki revision value. */
public record Revision(Instant timestampUtc, String username) {}
