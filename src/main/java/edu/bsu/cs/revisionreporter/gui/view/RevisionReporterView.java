package edu.bsu.cs.revisionreporter.gui.view;

import edu.bsu.cs.revisionreporter.model.Revision;
import edu.bsu.cs.revisionreporter.parse.RevisionFormatter;
import edu.bsu.cs.revisionreporter.parse.RevisionParser;
import edu.bsu.cs.revisionreporter.wikipedia.WikipediaClient;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

public final class RevisionReporterView {

    private static final int MAX_CHANGES = 15;
    private static final String UA_EMAIL = "braydon.hartwell@bsu.edu";

    private final RevisionParser parser = new RevisionParser();
    private final RevisionFormatter formatter = new RevisionFormatter();

    private TextField titleField;
    private TextArea output;

    public void start(Stage stage) {
        titleField = new TextField();
        titleField.setPromptText("Enter a Wikipedia page title (e.g., Bob Ross)");

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> onSearch());

        HBox top = new HBox(8, titleField, searchBtn);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(8));

        output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);

        VBox root = new VBox(8, top, output);
        root.setPadding(new Insets(8));

        stage.setTitle("Revision Reporter (GUI)");
        stage.setScene(new Scene(root, 960, 640));
        stage.show();
    }

    private void onSearch() {
        final String title = titleField.getText().trim();
        if (title.isBlank()) {
            output.setText("Please enter a Wikipedia article title.");
            return;
        }

        output.setText("Searching Wikipedia for: " + title + " …");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    WikipediaClient.StreamProvider provider = new WikipediaClient.LiveStreamProvider(UA_EMAIL);
                    InputStream in = provider.openStream(title, MAX_CHANGES);

                    RevisionParser.ParseResult result = parser.parse(in);
                    if (result.pageMissing()) return "No Wikipedia page found for \"" + title + "\".";

                    String finalTitle = result.redirectTo().orElse(title);

                    List<Revision> latest =
                            result.revisions().stream()
                                    .sorted(Comparator.comparing(Revision::timestampUtc).reversed())
                                    .limit(MAX_CHANGES)
                                    .toList();

                    return "=== " + finalTitle + " ===\n" +
                            formatter.formatNumbered(latest, MAX_CHANGES);

                } catch (Exception ex) {
                    return "Error while searching \"" + title + "\":\n" + ex.getMessage();
                }
            }
        };

        task.setOnSucceeded(e -> output.setText(task.getValue()));
        task.setOnFailed(e -> output.setText("Unexpected error: " + task.getException()));

        Thread t = new Thread(task, "wiki-fetch-thread");
        t.setDaemon(true);
        t.start();
    }
}
