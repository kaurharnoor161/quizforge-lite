package com.quiz.ui;

import com.quiz.Main;
import com.quiz.dao.JdbcQuestionDAO;
import com.quiz.dao.JdbcResultDAO;
import com.quiz.dao.QuestionDAO;
import com.quiz.dao.ResultDAO;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.Result;
import com.quiz.model.User;
import com.quiz.service.BackupService;
import com.quiz.util.AppLogger;
import com.quiz.util.FileUtil;
import com.quiz.util.QuestionValidator;
import com.quiz.util.ResultScoreComparator;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Admin dashboard: Tab 1 = Questions (Step 5), Tab 2 = Results + file handling (Step 6). */
public class AdminView {

    private static final String ALL_SUBJECTS = "All subjects";
    private static final String ALL_DIFFICULTIES = "All difficulties";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final User user;
    private final BorderPane root = new BorderPane();

    // data access + services
    private final QuestionDAO questionDao = new JdbcQuestionDAO();
    private final ResultDAO resultDao = new JdbcResultDAO();
    private final BackupService backupService = new BackupService(questionDao);

    // ---------- Questions tab controls ----------
    private final ObservableList<Question> questionList = FXCollections.observableArrayList();
    private final TableView<Question> questionTable = new TableView<>();
    private final ComboBox<String> subjectFilter = new ComboBox<>();
    private final ComboBox<String> difficultyFilter = new ComboBox<>();
    private final Label showingLabel = new Label();
    private boolean updatingFilters = false; // true while we fill the filter box by code

    private final ComboBox<String> subjectField = new ComboBox<>();
    private final TextArea questionField = new TextArea();
    private final TextField optionAField = new TextField();
    private final TextField optionBField = new TextField();
    private final TextField optionCField = new TextField();
    private final TextField optionDField = new TextField();
    private final ComboBox<String> correctField = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyField = new ComboBox<>();
    private final Button addButton = new Button("Add");
    private final Button updateButton = new Button("Update");
    private final Button deleteButton = new Button("Delete");
    private final Button clearButton = new Button("Clear form");
    private final Label statusLabel = new Label();

    // ---------- Results tab controls ----------
    private final ObservableList<Result> resultList = FXCollections.observableArrayList();
    private final TableView<Result> resultTable = new TableView<>();
    private final Label attemptsLabel = new Label("0 attempt(s)");

    public AdminView(User user) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        root.setTop(buildTopBar());
        root.setCenter(buildTabPane());
    }

    public BorderPane getRoot() {
        return root;
    }

    // =====================================================================
    // Top bar and tabs
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

    private TabPane buildTabPane() {
        TabPane tabPane = new TabPane();

        Tab questionsTab = new Tab("Questions", buildQuestionsPane());
        questionsTab.setClosable(false);

        Tab resultsTab = new Tab("Results", buildResultsPane());
        resultsTab.setClosable(false);
        // reload the results every time the admin opens this tab
        resultsTab.setOnSelectionChanged(e -> {
            if (resultsTab.isSelected()) {
                loadResults();
            }
        });

        tabPane.getTabs().addAll(questionsTab, resultsTab);
        return tabPane;
    }

    // =====================================================================
    // TAB 1: QUESTIONS (same behaviour as Step 5)
    // =====================================================================

    private BorderPane buildQuestionsPane() {
        BorderPane pane = new BorderPane();
        pane.setTop(buildFilterBar());
        pane.setCenter(buildQuestionTable());
        pane.setRight(buildForm());

        refreshSubjectLists();
        loadQuestions();
        return pane;
    }

    private HBox buildFilterBar() {
        difficultyFilter.getItems().addAll(ALL_DIFFICULTIES, "EASY", "MEDIUM", "HARD");
        difficultyFilter.setValue(ALL_DIFFICULTIES);
        subjectFilter.setValue(ALL_SUBJECTS);

        subjectFilter.setOnAction(e -> {
            if (!updatingFilters) {
                loadQuestions();
            }
        });
        difficultyFilter.setOnAction(e -> {
            if (!updatingFilters) {
                loadQuestions();
            }
        });

        Button clearFilters = new Button("Clear filters");
        clearFilters.setOnAction(e -> {
            updatingFilters = true;
            subjectFilter.setValue(ALL_SUBJECTS);
            difficultyFilter.setValue(ALL_DIFFICULTIES);
            updatingFilters = false;
            loadQuestions();
        });

        HBox bar = new HBox(10, new Label("Subject:"), subjectFilter,
                new Label("Difficulty:"), difficultyFilter, clearFilters, showingLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        return bar;
    }

    private TableView<Question> buildQuestionTable() {
        // The model has no JavaFX properties, so each cell value is wrapped by a lambda
        TableColumn<Question, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuestionId()));
        idCol.setPrefWidth(45);

        TableColumn<Question, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubject()));
        subjectCol.setPrefWidth(100);

        TableColumn<Question, String> textCol = new TableColumn<>("Question");
        textCol.setCellValueFactory(c ->
                new SimpleStringProperty(shorten(c.getValue().getQuestionText(), 90)));
        textCol.setPrefWidth(300);

        TableColumn<Question, String> correctCol = new TableColumn<>("Correct");
        correctCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getCorrectOption())));
        correctCol.setPrefWidth(60);

        TableColumn<Question, String> difficultyCol = new TableColumn<>("Difficulty");
        difficultyCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getDifficulty())));
        difficultyCol.setPrefWidth(85);

        questionTable.getColumns().add(idCol);
        questionTable.getColumns().add(subjectCol);
        questionTable.getColumns().add(textCol);
        questionTable.getColumns().add(correctCol);
        questionTable.getColumns().add(difficultyCol);
        questionTable.setItems(questionList);
        fitColumns(questionTable);

        // clicking a row fills the form
        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldQ, newQ) -> {
            if (newQ != null) {
                fillForm(newQ);
            }
        });
        return questionTable;
    }

    private ScrollPane buildForm() {
        subjectField.setEditable(true); // admin can type a brand-new subject
        questionField.setWrapText(true);
        questionField.setPrefRowCount(4);
        correctField.getItems().addAll("A", "B", "C", "D");
        difficultyField.getItems().addAll(Difficulty.values());

        subjectField.setMaxWidth(Double.MAX_VALUE);
        correctField.setMaxWidth(Double.MAX_VALUE);
        difficultyField.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        ColumnConstraints labelCol = new ColumnConstraints(75);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setPrefWidth(215);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        addFormRow(grid, 0, "Subject", subjectField);
        addFormRow(grid, 1, "Question", questionField);
        addFormRow(grid, 2, "Option A", optionAField);
        addFormRow(grid, 3, "Option B", optionBField);
        addFormRow(grid, 4, "Option C", optionCField);
        addFormRow(grid, 5, "Option D", optionDField);
        addFormRow(grid, 6, "Correct", correctField);
        addFormRow(grid, 7, "Difficulty", difficultyField);

        // Update and Delete only work when a row is selected
        updateButton.disableProperty().bind(questionTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(questionTable.getSelectionModel().selectedItemProperty().isNull());

        addButton.setOnAction(e -> handleAdd());
        updateButton.setOnAction(e -> handleUpdate());
        deleteButton.setOnAction(e -> handleDelete());
        clearButton.setOnAction(e -> clearForm());

        HBox buttons = new HBox(8, addButton, updateButton, deleteButton, clearButton);
        statusLabel.setStyle("-fx-text-fill: #1b6e2d;");
        statusLabel.setWrapText(true);

        VBox box = new VBox(10, grid, buttons, statusLabel);
        box.setPadding(new Insets(0, 10, 10, 10));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(350);
        scroll.setMinWidth(350);
        return scroll;
    }

    private void addFormRow(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        GridPane.setValignment(label, javafx.geometry.VPos.TOP);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    // ---------- Questions: data loading ----------

    /** Reloads the table using the current filters. */
    private void loadQuestions() {
        try {
            String subject = subjectFilter.getValue();
            if (subject == null || ALL_SUBJECTS.equals(subject)) {
                subject = null;
            }
            String diffText = difficultyFilter.getValue();
            Difficulty difficulty = null;
            if (diffText != null && !ALL_DIFFICULTIES.equals(diffText)) {
                difficulty = Difficulty.fromString(diffText);
            }

            List<Question> found = questionDao.findByFilter(subject, difficulty);
            questionList.setAll(found);
            showingLabel.setText("Showing " + found.size() + " question(s)");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    /** Refills the subject filter and the form's subject box. Keeps the chosen filter if it still exists. */
    private void refreshSubjectLists() {
        try {
            Set<String> distinct = questionDao.getDistinctSubjects(); // a HashSet
            List<String> subjects = new ArrayList<>(distinct);
            Collections.sort(subjects);

            String currentFilter = subjectFilter.getValue();

            updatingFilters = true; // do not reload the table while we change the box
            subjectFilter.getItems().clear();
            subjectFilter.getItems().add(ALL_SUBJECTS);
            subjectFilter.getItems().addAll(subjects);
            if (currentFilter != null && subjects.contains(currentFilter)) {
                subjectFilter.setValue(currentFilter);
            } else {
                subjectFilter.setValue(ALL_SUBJECTS); // subject no longer exists
            }
            updatingFilters = false;

            String typed = subjectField.getEditor().getText();
            subjectField.getItems().setAll(subjects);
            subjectField.getEditor().setText(typed);
        } catch (SQLException ex) {
            updatingFilters = false;
            showDatabaseError(ex);
        }
    }

    /** Used by the Results tab after a restore: everything on the Questions tab is reloaded. */
    private void refreshQuestionsTab() {
        refreshSubjectLists();
        loadQuestions();
        clearForm();
    }

    // ---------- Questions: form handling ----------

    private void fillForm(Question q) {
        subjectField.setValue(q.getSubject());
        questionField.setText(q.getQuestionText());
        optionAField.setText(q.getOptionA());
        optionBField.setText(q.getOptionB());
        optionCField.setText(q.getOptionC());
        optionDField.setText(q.getOptionD());
        correctField.setValue(String.valueOf(q.getCorrectOption()));
        difficultyField.setValue(q.getDifficulty());
        statusLabel.setText("");
    }

    private void clearForm() {
        questionTable.getSelectionModel().clearSelection();
        subjectField.setValue(null);
        subjectField.getEditor().clear();
        questionField.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        correctField.setValue(null);
        difficultyField.setValue(null);
        statusLabel.setText("");
    }

    /** Builds a validated Question from the form (throws IllegalArgumentException if something is wrong). */
    private Question questionFromForm() {
        return QuestionValidator.createQuestion(
                subjectField.getEditor().getText(),
                questionField.getText(),
                optionAField.getText(),
                optionBField.getText(),
                optionCField.getText(),
                optionDField.getText(),
                correctField.getValue(),
                difficultyField.getValue());
    }

    private void handleAdd() {
        try {
            Question question = questionFromForm();
            questionDao.insert(question);
            // the DAO has set the generated id on the object, so this never logs id=0
            AppLogger.info("Admin '" + user.getUsername() + "' added question id=" + question.getQuestionId());

            refreshSubjectLists();
            loadQuestions();
            clearForm();
            statusLabel.setText("Question added.");
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid question", ex.getMessage(), null);
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void handleUpdate() {
        Question selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            Question question = questionFromForm();
            question.setQuestionId(selected.getQuestionId());
            questionDao.update(question);
            AppLogger.info("Admin '" + user.getUsername() + "' updated question id=" + question.getQuestionId());

            refreshSubjectLists();
            loadQuestions();
            clearForm();
            statusLabel.setText("Question updated.");
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid question", ex.getMessage(), null);
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void handleDelete() {
        Question selected = questionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int id = selected.getQuestionId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this question?\n\n\"" + shorten(selected.getQuestionText(), 80) + "\"",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText(null);
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return; // Cancel: do nothing
        }

        try {
            questionDao.delete(id);
            AppLogger.info("Admin '" + user.getUsername() + "' deleted question id=" + id);

            refreshSubjectLists();
            loadQuestions();
            clearForm();
            statusLabel.setText("Question deleted.");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    // =====================================================================
    // TAB 2: RESULTS + FILE HANDLING (Step 6)
    // =====================================================================

    private BorderPane buildResultsPane() {
        // results buttons | separator | question-bank buttons | spacer | counter
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadResults());

        Button exportButton = new Button("Export results to CSV");
        exportButton.setOnAction(this::handleExport);

        Button backupButton = new Button("Backup questions");
        backupButton.setOnAction(this::handleBackup);

        Button restoreButton = new Button("Restore questions");
        restoreButton.setOnAction(this::handleRestore);

        Separator separator = new Separator(Orientation.VERTICAL);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(10, refreshButton, exportButton, separator,
                backupButton, restoreButton, spacer, attemptsLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        BorderPane pane = new BorderPane();
        pane.setTop(top);
        pane.setCenter(buildResultTable());

        loadResults(); // load once when the tab is built
        return pane;
    }

    private TableView<Result> buildResultTable() {
        TableColumn<Result, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStudentName()));
        studentCol.setPrefWidth(200);

        TableColumn<Result, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubject()));
        subjectCol.setPrefWidth(150);

        TableColumn<Result, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getScore()));
        scoreCol.setPrefWidth(70);

        TableColumn<Result, Number> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getTotal()));
        totalCol.setPrefWidth(70);

        // numeric column that is DISPLAYED with a % sign (so sorting still works as numbers)
        TableColumn<Result, Number> percentCol = new TableColumn<>("Percentage");
        percentCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPercentage()));
        percentCol.setCellFactory(col -> new TableCell<Result, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + "%");
            }
        });
        percentCol.setPrefWidth(90);

        TableColumn<Result, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAttemptedOnText()));
        dateCol.setPrefWidth(190);

        resultTable.getColumns().add(studentCol);
        resultTable.getColumns().add(subjectCol);
        resultTable.getColumns().add(scoreCol);
        resultTable.getColumns().add(totalCol);
        resultTable.getColumns().add(percentCol);
        resultTable.getColumns().add(dateCol);
        resultTable.setItems(resultList);
        resultTable.setPlaceholder(new Label("No results yet"));
        fitColumns(resultTable);
        return resultTable;
    }

    /** Loads results from the database, copies them, sorts them with the Comparator, shows them. */
    private void loadResults() {
        try {
            List<Result> results = new ArrayList<>(resultDao.findAllWithStudentNames());
            Collections.sort(results, new ResultScoreComparator());
            resultList.setAll(results);
            attemptsLabel.setText(results.size() + " attempt(s)");
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void handleExport(ActionEvent event) {
        Window owner = ownerOf(event);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export results to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        chooser.setInitialFileName("results_" + LocalDate.now().format(FILE_DATE) + ".csv");

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return; // cancelled
        }
        file = ensureExtension(file, ".csv");

        if (resultList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Nothing to export",
                    "There are no results to export yet", owner);
            return;
        }

        try {
            List<Result> rows = new ArrayList<>(resultList); // the displayed, sorted list
            FileUtil.writeResultsCsv(rows, file);
            AppLogger.info("Admin '" + user.getUsername() + "' exported " + rows.size()
                    + " result(s) to " + file.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Export complete",
                    "Saved to:\n" + file.getAbsolutePath() + "\n\nRows exported: " + rows.size(), owner);
        } catch (IOException ex) {
            AppLogger.severe("CSV export failed for admin '" + user.getUsername() + "'", ex);
            showAlert(Alert.AlertType.ERROR, "Could not write the file",
                    "The CSV file could not be saved. Check that the folder is writable and try again.", owner);
        }
    }

    private void handleBackup(ActionEvent event) {
        Window owner = ownerOf(event);

        FileChooser chooser = backupChooser("Backup questions");
        chooser.setInitialFileName("quizforge_backup_" + LocalDate.now().format(FILE_DATE) + ".dat");

        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return; // cancelled
        }
        file = ensureExtension(file, ".dat");

        try {
            int count = backupService.backupQuestions(file);
            AppLogger.info("Admin '" + user.getUsername() + "' backed up " + count
                    + " question(s) to " + file.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Backup complete",
                    count + " questions saved to " + file.getAbsolutePath(), owner);
        } catch (SQLException ex) {
            AppLogger.severe("Backup failed (database) for admin '" + user.getUsername() + "'", ex);
            showAlert(Alert.AlertType.ERROR, "Database error",
                    "The questions could not be read from the database. No backup file was made.", owner);
        } catch (IOException ex) {
            AppLogger.severe("Backup failed (file) for admin '" + user.getUsername() + "'", ex);
            showAlert(Alert.AlertType.ERROR, "Could not write the backup file",
                    "The backup file could not be saved. Check that the folder is writable and try again.", owner);
        }
    }

    private void handleRestore(ActionEvent event) {
        Window owner = ownerOf(event);

        FileChooser chooser = backupChooser("Restore questions");
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return; // cancelled
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will REPLACE all current questions with the ones in the backup file. "
                        + "This cannot be undone. Continue?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Confirm restore");
        confirm.setHeaderText(null);
        confirm.initOwner(owner);
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return; // Cancel: nothing changes
        }

        try {
            BackupService.RestoreSummary summary = backupService.restoreQuestions(file);
            AppLogger.info("Admin '" + user.getUsername() + "' restored questions from "
                    + file.getAbsolutePath() + ": restored=" + summary.getRestoredCount()
                    + ", skipped=" + summary.getSkippedCount());
            showAlert(Alert.AlertType.INFORMATION, "Restore complete",
                    "Questions restored: " + summary.getRestoredCount()
                            + "\nInvalid entries skipped: " + summary.getSkippedCount()
                            + "\n\n(Question ids change after a restore. This is normal.)", owner);
            refreshQuestionsTab();
        } catch (IllegalArgumentException ex) {
            AppLogger.warning("Restore refused for admin '" + user.getUsername() + "': " + ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Restore refused", ex.getMessage(), owner);
        } catch (IOException | ClassNotFoundException ex) {
            AppLogger.severe("Could not read backup file " + file.getAbsolutePath(), ex);
            showAlert(Alert.AlertType.ERROR, "Could not read the backup file",
                    "The chosen file is not a QuizForge backup, or it is damaged. "
                            + "Please pick a .dat file made with \"Backup questions\". "
                            + "Your questions were not changed.", owner);
        } catch (SQLException ex) {
            AppLogger.severe("Restore failed (database) for admin '" + user.getUsername() + "'", ex);
            showAlert(Alert.AlertType.ERROR, "Database error",
                    "The restore failed. Nothing was changed because the transaction was rolled back.", owner);
        }
    }

    // =====================================================================
    // Small helpers
    // =====================================================================

    private FileChooser backupChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("QuizForge backup (*.dat)", "*.dat"));
        return chooser;
    }

    /** The window that contains the clicked button (used as owner of dialogs). */
    private Window ownerOf(ActionEvent event) {
        return ((Node) event.getSource()).getScene().getWindow();
    }

    /** macOS save dialogs do not always add the extension, so we add it if it is missing. */
    private File ensureExtension(File file, String extension) {
        if (file.getName().toLowerCase().endsWith(extension)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + extension);
    }

    private void showAlert(Alert.AlertType type, String title, String message, Window owner) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    private void showDatabaseError(SQLException ex) {
        AppLogger.severe("Database error in AdminView (admin '" + user.getUsername() + "')", ex);
        showAlert(Alert.AlertType.ERROR, "Database error",
                "Something went wrong while talking to the database:\n" + ex.getMessage(), null);
    }

    /** Cuts long text and adds "..." so table cells do not stretch. */
    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').replace('\r', ' ');
        if (oneLine.length() <= maxLength) {
            return oneLine;
        }
        return oneLine.substring(0, maxLength - 3) + "...";
    }

    /** Columns share the table width, so there is no horizontal scrolling. */
    @SuppressWarnings("deprecation")
    private void fitColumns(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}