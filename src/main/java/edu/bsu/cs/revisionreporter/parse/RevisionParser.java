package edu.bsu.cs.revisionreporter.parse;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import edu.bsu.cs.revisionreporter.model.Revision;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RevisionParser {

    public static final class ParseResult {
        private final List<Revision> revisions;
        private final Optional<String> redirectTo;
        private final boolean pageMissing;

        public ParseResult(List<Revision> revisions, Optional<String> redirectTo, boolean pageMissing) {
            this.revisions = revisions;
            this.redirectTo = redirectTo;
            this.pageMissing = pageMissing;
        }

        public List<Revision> revisions() { return revisions; }
        public Optional<String> redirectTo() { return redirectTo; }
        public boolean pageMissing() { return pageMissing; }
    }

    @SuppressWarnings("unchecked")
    public ParseResult parse(InputStream jsonStream) throws IOException {
        String json = new String(jsonStream.readAllBytes(), StandardCharsets.UTF_8);
        DocumentContext ctx = JsonPath.parse(json);

        Optional<String> redirectTo = Optional.empty();
        List<Map<String, Object>> redirects = null;
        try { redirects = ctx.read("$.query.redirects", List.class); } catch (Exception ignored) {}
        if (redirects != null && !redirects.isEmpty()) {
            Object to = redirects.get(0).get("to");
            if (to != null) redirectTo = Optional.of(to.toString());
        }

        Map<String, Object> pages = null;
        try { pages = ctx.read("$.query.pages", Map.class); } catch (Exception ignored) {}

        boolean missing = false;
        List<Map<String, Object>> revisionMaps = new ArrayList<>();

        if (pages != null && !pages.isEmpty()) {
            for (Map.Entry<String, Object> entry : pages.entrySet()) {
                Map<String, Object> page = (Map<String, Object>) entry.getValue();
                List<Map<String, Object>> revs = (List<Map<String, Object>>) page.get("revisions");
                if ("-1".equals(entry.getKey()) || page.containsKey("missing") || revs == null) {
                    missing = true;
                } else {
                    revisionMaps = revs;
                }
                break; // use the first page
            }
        } else {
            missing = true;
        }

        List<Revision> revisions = new ArrayList<>();
        for (Map<String, Object> m : revisionMaps) {
            String user = String.valueOf(m.get("user"));
            String ts = String.valueOf(m.get("timestamp"));
            revisions.add(new Revision(Instant.parse(ts), user));
        }

        return new ParseResult(revisions, redirectTo, missing);
    }
}

