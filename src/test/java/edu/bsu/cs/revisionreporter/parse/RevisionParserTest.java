
package edu.bsu.cs.revisionreporter.parse;

import edu.bsu.cs.revisionreporter.model.Revision;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RevisionParserTest {

    @Test
    void parsesRedirectAndRevisions() throws Exception {
        try (InputStream in = resource("samplejson/redirect.json")) {
            RevisionParser parser = new RevisionParser();
            RevisionParser.ParseResult result = parser.parse(in);
            assertTrue(result.redirectTo().isPresent());
            assertEquals("Frank Zappa", result.redirectTo().get());
            List<Revision> revisions = result.revisions();
            assertEquals(2, revisions.size());
            assertEquals("UserA", revisions.get(0).username());
        }
    }

    @Test
    void detectsMissingPage() throws Exception {
        try (InputStream in = resource("samplejson/missing.json")) {
            RevisionParser parser = new RevisionParser();
            RevisionParser.ParseResult result = parser.parse(in);
            assertTrue(result.pageMissing());
            assertEquals(0, result.revisions().size());
        }
    }

    private InputStream resource(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }
}
