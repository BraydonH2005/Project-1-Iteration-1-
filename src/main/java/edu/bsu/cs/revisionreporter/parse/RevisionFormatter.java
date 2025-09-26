
package edu.bsu.cs.revisionreporter.parse;

import edu.bsu.cs.revisionreporter.model.Revision;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class RevisionFormatter {
    private static final DateTimeFormatter ISO_8601_UTC = DateTimeFormatter.ISO_INSTANT;

    public String formatNumbered(List<Revision> revisions, int max) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(max, revisions.size());
        for (int i = 0; i < count; i++) {
            Revision r = revisions.get(i);
            sb.append(i + 1)
              .append("  ")
              .append(ISO_8601_UTC.format(r.timestampUtc()))
              .append("  ")
              .append(r.username())
              .append(System.lineSeparator());
        }
        return sb.toString();
    }
}
