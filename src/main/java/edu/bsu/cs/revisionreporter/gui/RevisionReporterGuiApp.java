package edu.bsu.cs.revisionreporter.gui;

import edu.bsu.cs.revisionreporter.model.Revision;
import edu.bsu.cs.revisionreporter.parse.RevisionFormatter;
import edu.bsu.cs.revisionreporter.parse.RevisionParser;
import edu.bsu.cs.revisionreporter.wikipedia.WikipediaClient;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

public final class RevisionReporterGuiApp extends Application {

    private static final int MAX_CHANGES = 15;
    private static final String UA_EMAIL = "braydon.hartwell@bsu.edu";

    private final RevisionParser parser = new RevisionParser();
    private final RevisionFormatter formatter = new RevisionFormatter();

    private TextField titleField;
    private TextArea output;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Revision Reporter (GUI)");

        Label lbl = new Label("Title:");
        titleField = new TextField();
        titleField.setPromptText("e.g., Bob Ross");
        titleField.setPrefColumnCount(42);

        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> runSearch());
        titleField.setOnAction(e -> runSearch());

        HBox top = new HBox(8, lbl, titleField, searchBtn);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleField, Priority.ALWAYS);

        output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setStyle("-fx-font-family: 'Consolas', 'Menlo', monospace; -fx-font-size: 12px;");

        VBox root = new VBox(10, top, output);
        root.setPadding(new Insets(12));
        VBox.setVgrow(output, Priority.ALWAYS);

        stage.setScene(new Scene(root, 900, 560));
        stage.show();
    }

    private void runSearch() {
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

                    // 1) fetch
                    InputStream in = provider.openStream(title, MAX_CHANGES);

                    // 2) parse
                    RevisionParser.ParseResult result = parser.parse(in);
                    if (result.pageMissing()) {
                        return "No Wikipedia page found for \"" + title + "\".";
                    }

                    String finalTitle = result.redirectTo().orElse(title);

                    // newest-first & up to 15
                    List<Revision> latest =
                            result.revisions().stream()
                                    .sorted(Comparator.comparing(Revision::timestampUtc).reversed())
                                    .limit(MAX_CHANGES)
                                    .toList();

                    // 3) format
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

    public static void main(String[] args) {
        launch(args);
    }
}
