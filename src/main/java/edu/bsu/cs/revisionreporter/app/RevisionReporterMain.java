
package edu.bsu.cs.revisionreporter.app;

import edu.bsu.cs.revisionreporter.model.Revision;
import edu.bsu.cs.revisionreporter.parse.RevisionFormatter;
import edu.bsu.cs.revisionreporter.parse.RevisionParser;
import edu.bsu.cs.revisionreporter.wikipedia.WikipediaClient;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class RevisionReporterMain {

    private static final int MAX_CHANGES = 15;
    private static final String UA_EMAIL = "braydon.hartwell@bsu.edu";

    public static void main(String[] args) {
        boolean offline = false;
        Path offlineJson = null;
        String cliTitle = null;

        for (String a : args) {
            if (a.equalsIgnoreCase("--offline")) offline = true;
            else if (a.startsWith("--file=")) offlineJson = Path.of(a.substring("--file=".length()));
            else if (a.startsWith("--title=")) cliTitle = a.substring("--title=".length()).trim();
        }

        String title = cliTitle;
        if (title == null || title.isEmpty()) {
            if (System.console() == null) {
                System.err.println("Error: No article name provided.");
                System.err.println("Tip: use --title=<Article> (e.g., --title='Frank Zappa')");
                return;
            }
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Enter Wikipedia article title: ");
                if (scanner.hasNextLine()) {
                    title = scanner.nextLine().trim();
                } else {
                    System.err.println("Error: No input received. Use --title=<Article>.");
                    return;
                }
            }
        }
        if (title.isEmpty()) {
            System.err.println("Error: No article name provided.");
            return;
        }

        RevisionParser parser = new RevisionParser();
        try (InputStream in = openInputStream(offline, offlineJson, title)) {
            RevisionParser.ParseResult result = parser.parse(in);

            if (result.pageMissing()) {
                System.err.println("Error: No Wikipedia page found for that title.");
                return;
            }

            Optional<String> redirect = result.redirectTo();
            String finalTitle = redirect.orElse(title);
            redirect.ifPresent(target -> System.out.println("Redirected to " + target));

            // Subject info
            System.out.println("Subject: " + finalTitle);
            if (!offline) {
                try (InputStream intro = openIntro(finalTitle)) {
                    String introText = extractIntro(intro);
                    if (!introText.isBlank()) {
                        String firstPara = introText.split("\r?\n\r?\n|\r?\n", 2)[0].trim();
                        if (!firstPara.isBlank()) System.out.println(firstPara);
                    }
                } catch (IOException ignored) { }
            }

            // Revisions: newest first, print up to 15
            List<Revision> revisions = result.revisions()
                    .stream()
                    .sorted(Comparator.comparing(Revision::timestampUtc).reversed())
                    .toList();

            int shown = Math.min(MAX_CHANGES, revisions.size());
            System.out.println("Edits (up to " + MAX_CHANGES + "): " + revisions.size());
            System.out.println("Most recent " + shown + " changes:");

            RevisionFormatter formatter = new RevisionFormatter();
            System.out.print(formatter.formatNumbered(revisions, MAX_CHANGES));

        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
            System.err.println("Tip: run with --offline --file=src/test/resources/samplejson/redirect.json if needed.");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static InputStream openInputStream(boolean offline, Path offlineJson, String title) throws IOException {
        if (offline) {
            if (offlineJson == null) offlineJson = Path.of("src/test/resources/samplejson/redirect.json");
            if (!Files.exists(offlineJson)) {
                throw new IOException("Offline JSON file not found: " + offlineJson.toAbsolutePath());
            }
            return new FileInputStream(offlineJson.toFile());
        } else {
            WikipediaClient.StreamProvider provider =
                new WikipediaClient.LiveStreamProvider(UA_EMAIL);
            try {
                return provider.openStream(title, MAX_CHANGES);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", ie);
            }
        }
    }

    private static InputStream openIntro(String finalTitle) throws IOException {
        WikipediaClient.StreamProvider provider =
            new WikipediaClient.LiveStreamProvider(UA_EMAIL);
        try {
            return provider.openIntroStream(finalTitle);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", ie);
        }
    }

    private static String extractIntro(InputStream introJson) throws IOException {
        String json = new String(introJson.readAllBytes(), StandardCharsets.UTF_8);
        DocumentContext ctx = JsonPath.parse(json);
        try {
            String extract = ctx.read("$.query.pages.*.extract");
            return extract == null ? "" : extract.trim();
        } catch (Exception e) {
            return "";
        }
    }
}
