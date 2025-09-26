
package edu.bsu.cs.revisionreporter.parse;

import edu.bsu.cs.revisionreporter.model.Revision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RevisionParserCelebTopicsTest {

    @Test
    @DisplayName("Parses Taylor Swift with redirect and 3 revisions")
    void taylorSwift() throws Exception {
        try (InputStream in = resource("samplejson/taylor_swift.json")) {
            RevisionParser.ParseResult result = new RevisionParser().parse(in);
            assertFalse(result.pageMissing());
            assertTrue(result.redirectTo().isPresent());
            assertEquals("Taylor Swift", result.redirectTo().get());
            List<Revision> revs = result.revisions();
            assertEquals(3, revs.size());
            assertEquals("EditorA", revs.get(0).username());
        }
    }

    @Test
    @DisplayName("Parses Albert Einstein with 1 revision and no redirect")
    void albertEinstein() throws Exception {
        try (InputStream in = resource("samplejson/albert_einstein.json")) {
            RevisionParser.ParseResult result = new RevisionParser().parse(in);
            assertFalse(result.pageMissing());
            assertTrue(result.redirectTo().isEmpty());
            assertEquals(1, result.revisions().size());
            assertEquals("HistUser1", result.revisions().get(0).username());
        }
    }

    @Test
    @DisplayName("Parses The Beatles with 2 revisions")
    void theBeatles() throws Exception {
        try (InputStream in = resource("samplejson/the_beatles.json")) {
            RevisionParser.ParseResult result = new RevisionParser().parse(in);
            assertFalse(result.pageMissing());
            assertEquals(2, result.revisions().size());
        }
    }

    @Test
    @DisplayName("Parses World War II with 4 revisions")
    void worldWarII() throws Exception {
        try (InputStream in = resource("samplejson/world_war_ii.json")) {
            RevisionParser.ParseResult result = new RevisionParser().parse(in);
            assertFalse(result.pageMissing());
            assertEquals(4, result.revisions().size());
            assertEquals("Historian42", result.revisions().get(0).username());
        }
    }

    @Test
    @DisplayName("Parses Python (programming language) with redirect and 2 revisions")
    void pythonLanguage() throws Exception {
        try (InputStream in = resource("samplejson/python_programming_language.json")) {
            RevisionParser.ParseResult result = new RevisionParser().parse(in);
            assertFalse(result.pageMissing());
            assertTrue(result.redirectTo().isPresent());
            assertEquals("Python (programming language)", result.redirectTo().get());
            assertEquals(2, result.revisions().size());
        }
    }

    private InputStream resource(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }
}
