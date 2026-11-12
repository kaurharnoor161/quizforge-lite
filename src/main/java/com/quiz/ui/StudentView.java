package com.quiz.ui;

import com.quiz.Main;
import com.quiz.exception.NotEnoughQuestionsException;
import com.quiz.model.Question;
import com.quiz.model.Result;
import com.quiz.model.User;
import com.quiz.service.QuizService;
import com.quiz.service.StudentService;
import com.quiz.util.AppLogger;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;

/** Student home screen: choose a subject, start a quiz, see the last 5 attempts and the full history. */
public class StudentView {

    private final User user;
    private final StudentService studentService = new StudentService();
    private final QuizService quizService = new QuizService();
    private final BorderPane root = new BorderPane();

    // left panel
    private final ObservableList<String> subjectItems = FXCollections.observableArrayList();
    private final ListView<String> subjectList = new ListView<>(subjectItems);
    private final Label infoLabel = new Label();
    private final CheckBox shuffleBox = new CheckBox("Shuffle questions");
    private final Button startButton = new Button("Start quiz");

    // centre panel
    private final ObservableList<String> recentItems = FXCollections.observableArrayList();
    private final ListView<String> recentList = new ListView<>(recentItems);
    private final ObservableList<Result> historyItems = FXCollections.observableArrayList();
    private final TableView<Result> historyTable = new TableView<>();
    private final Label historyLabel = new Label("All your attempts (0)");

    public StudentView(User user) {
        this.user = Objects.requireNonNull(user, "user must not be null");

        root.setTop(buildTopBar());
        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenterPanel());

        refresh(); // load everything once when the screen is built
    }

    public BorderPane getRoot() {
        return root;
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private HBox buildTopBar() {
        Label title = new Label(user.getDashboardTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label welcome = new Label("Welcome, " + user.getFullName());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> Main.logout());

        HBox bar = new HBox(15, title, spacer, welcome, logoutButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 15, 10, 15));
        bar.setStyle("-fx-background-color: #eef2f7; -fx-border-color: #c9d1dc; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private VBox buildLeftPanel() {
        Label heading = new Label("1. Choose a subject");
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        subjectList.setPlaceholder(new Label("No questions available yet. Ask the admin to add some."));
        VBox.setVgrow(subjectList, Priority.ALWAYS);

        infoLabel.setWrapText(true);

        // unchecked = questions shown in id order, checked = questions shown in random order
        shuffleBox.setSelected(false);

        // Start is disabled while nothing is selected
        startButton.disableProperty().bind(subjectList.getSelectionModel().selectedItemProperty().isNull());
        startButton.setStyle("-fx-font-weight: bold;");
        startButton.setOnAction(e -> handleStart());

        // selecting a subject shows how many questions it has
        subjectList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateInfoLabel());

        VBox box = new VBox(10, heading, subjectList, infoLabel, shuffleBox, startButton);
        box.setPadding(new Insets(15));
        box.setPrefWidth(300);
        box.setMinWidth(300);
        box.setMaxWidth(300);
        box.setStyle("-fx-background-color: #f7f9fb; -fx-border-color: #c9d1dc; -fx-border-width: 0 1 0 0;");
        return box;
    }

    private VBox buildCenterPanel() {
        Label recentHeading = new Label("Your last 5 attempts");
        recentHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headingRow = new HBox(10, recentHeading, spacer, refreshButton);
        headingRow.setAlignment(Pos.CENTER_LEFT);

        recentList.setPlaceholder(new Label("No attempts yet"));
        recentList.setPrefHeight(150);
        recentList.setMinHeight(150);
        recentList.setMaxHeight(150);

        historyLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        buildHistoryTable();
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        VBox box = new VBox(10, headingRow, recentList, historyLabel, historyTable);
        box.setPadding(new Insets(15));
        return box;
    }

    private void buildHistoryTable() {
        // The model has no JavaFX properties, so lambdas wrap each value
        TableColumn<Result, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubject()));
        subjectCol.setPrefWidth(150);

        TableColumn<Result, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getScore() + " / " + c.getValue().getTotal()));
        scoreCol.setPrefWidth(90);

        TableColumn<Result, String> percentCol = new TableColumn<>("Percentage");
        percentCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPercentage() + "%"));
        percentCol.setPrefWidth(100);

        TableColumn<Result, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAttemptedOnText()));
        dateCol.setPrefWidth(220);

        historyTable.getColumns().add(subjectCol);
        historyTable.getColumns().add(scoreCol);
        historyTable.getColumns().add(percentCol);
        historyTable.getColumns().add(dateCol);
        historyTable.setItems(historyItems);
        historyTable.setPlaceholder(new Label("You have not taken any quiz yet."));
        fitColumns(historyTable);
    }

    // =====================================================================
    // Loading data
    // =====================================================================

    /** Reloads the subject list (keeping the selection), the last-5 list and the history table. */
    public void refresh() {
        try {
            String selected = subjectList.getSelectionModel().getSelectedItem();

            subjectItems.setAll(studentService.getSubjects()); // this clears the selection...
            if (selected != null && subjectItems.contains(selected)) {
                subjectList.getSelectionModel().select(selected); // ...so we select it again
            }
            updateInfoLabel();

            loadAttempts();
        } catch (IllegalStateException ex) {
            showDatabaseError(ex);
        }
    }

    private void loadAttempts() {
        int userId = user.getUserId();

        // last 5 (built with a LinkedList in the service)
        recentItems.clear();
        for (Result r : studentService.getRecentAttempts(userId, 5)) {
            recentItems.add(r.getSubject() + " - " + r.getScore() + "/" + r.getTotal()
                    + " (" + r.getPercentage() + "%) - " + r.getAttemptedOnText());
        }

        // all attempts, newest first
        List<Result> history = studentService.getHistory(userId);
        historyItems.setAll(history);
        historyLabel.setText("All your attempts (" + history.size() + ")");
    }

    private void updateInfoLabel() {
        String text = "Each quiz has " + studentService.getQuizSize() + " questions";
        String subject = subjectList.getSelectionModel().getSelectedItem();
        if (subject != null) {
            try {
                text += "\n" + subject + ": " + studentService.getQuestionCount(subject) + " questions available";
            } catch (IllegalStateException ex) {
                showDatabaseError(ex);
            }
        }
        infoLabel.setText(text);
    }

    // =====================================================================
    // Start button
    // =====================================================================

    private void handleStart() {
        String subject = subjectList.getSelectionModel().getSelectedItem();
        if (subject == null) { // the button is disabled, but we guard anyway
            showAlert(Alert.AlertType.WARNING, "No subject selected", "Please choose a subject first.");
            return;
        }

        try {
            int available = studentService.getQuestionCount(subject);
            int needed = studentService.getQuizSize();

            if (available < needed) {
                String message = "The subject \"" + subject + "\" has only " + available
                        + " questions, but a quiz needs " + needed
                        + ". Please ask the admin to add more questions.";
                AppLogger.warning(user.getUsername() + " tried to start " + subject
                        + " but it has only " + available + " of " + needed + " questions");
                showAlert(Alert.AlertType.WARNING, "Not enough questions", message);
                return;
            }

            boolean shuffle = shuffleBox.isSelected();
            AppLogger.info(user.getUsername() + " chose subject " + subject + " (shuffle=" + shuffle + ")");
            startQuiz(subject, shuffle);
        } catch (IllegalStateException ex) {
            showDatabaseError(ex);
        }
    }

    /** Builds the question paper and opens the quiz in its own modal window. */
    private void startQuiz(String subject, boolean shuffle) {
        try {
            List<Question> paper = quizService.buildPaper(subject, shuffle);
            Window owner = startButton.getScene().getWindow(); // the window of the Start button
            // this::refresh runs after a finished quiz, so the new attempt shows up at once
            new QuizView(user, subject, paper, owner, this::refresh).show();
        } catch (NotEnoughQuestionsException ex) {
            AppLogger.warning(user.getUsername() + " could not start " + subject + ": " + ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Not enough questions", ex.getMessage());
        } catch (IllegalStateException ex) {
            showDatabaseError(ex);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showDatabaseError(IllegalStateException ex) {
        // the service has already logged the SQLException with AppLogger.severe
        showAlert(Alert.AlertType.ERROR, "Database error",
                "Something went wrong while talking to the database. Please try again.");
    }

    /** Columns share the table width, so there is no horizontal scrolling. */
    @SuppressWarnings("deprecation")
    private void fitColumns(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}