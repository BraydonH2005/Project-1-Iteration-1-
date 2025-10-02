package edu.bsu.cs.revisionreporter.wikipedia;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * HTTP client for Wikipedia's MediaWiki API (stream-based).
 * Keep parameter names (rvprop, rvlimit, etc.) exactly as the API expects.
 */
@SuppressWarnings("SpellCheckingInspection")
public final class WikipediaClient {

    public interface StreamProvider {
        InputStream openStream(String article, int limit) throws IOException, InterruptedException;
        InputStream openIntroStream(String article) throws IOException, InterruptedException;
    }

    public static final class LiveStreamProvider implements StreamProvider {
        private final String userAgentEmail;
        private final HttpClient client = HttpClient.newHttpClient();

        public LiveStreamProvider(String userAgentEmail) {
            this.userAgentEmail = userAgentEmail;
        }

        @Override
        public InputStream openStream(String article, int limit) throws IOException, InterruptedException {
            String encodedTitle = URLEncoder.encode(article, StandardCharsets.UTF_8);
            URI uri = URI.create(buildRevisionsUrl(encodedTitle, limit));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("User-Agent", "Revision Reporter/0.1 (" + userAgentEmail + ")")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) throw new IOException("HTTP " + response.statusCode() + " from Wikipedia");
            return response.body();
        }

        @Override
        public InputStream openIntroStream(String article) throws IOException, InterruptedException {
            String encodedTitle = URLEncoder.encode(article, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/w/api.php?action=query&format=json"
                    + "&prop=extracts&exintro&explaintext&redirects&titles=" + encodedTitle;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Revision Reporter/0.1 (" + userAgentEmail + ")")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) throw new IOException("HTTP " + response.statusCode() + " from Wikipedia");
            return response.body();
        }

        static String buildRevisionsUrl(String encodedTitle, int limit) {
            // Encode the pipe for JDK 23+ URI validity
            String rvprop = URLEncoder.encode("timestamp|user", StandardCharsets.UTF_8);
            return "https://en.wikipedia.org/w/api.php"
                    + "?action=query"
                    + "&format=json"
                    + "&prop=revisions"
                    + "&titles=" + encodedTitle
                    + "&rvprop=" + rvprop
                    + "&rvlimit=" + limit
                    + "&redirects";
        }
    }
}
