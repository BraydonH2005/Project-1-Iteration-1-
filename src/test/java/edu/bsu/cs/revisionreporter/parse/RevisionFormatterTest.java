
package edu.bsu.cs.revisionreporter.parse;

import edu.bsu.cs.revisionreporter.model.Revision;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class RevisionFormatterTest {
    @Test
    void printsNumberedIsoLines() {
        var r1 = new Revision(Instant.parse("2025-08-13T22:47:03Z"), "UserA");
        var r2 = new Revision(Instant.parse("2025-08-13T20:25:31Z"), "UserB");
        var out = new RevisionFormatter().formatNumbered(List.of(r1, r2), 15);
        assertTrue(out.startsWith("1  "));
        assertTrue(out.contains("2025-08-13T22:47:03Z  UserA"));
        assertTrue(out.contains("2  2025-08-13T20:25:31Z  UserB"));
    }
}
