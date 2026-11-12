package com.quiz.ui;

import com.quiz.model.Question;
import com.quiz.model.Result;
import com.quiz.model.User;
import com.quiz.service.QuizService;
import com.quiz.util.AppLogger;
import com.quiz.util.ConfigLoader;
import com.quiz.util.Countdown;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The quiz window (own modal window) with an optional countdown timer. */
public class QuizView {

    private final User user;
    private final String subject;
    private final List<Question> paper;
    private final Window owner;
    private final Runnable onFinished;
    private final QuizService quizService = new QuizService();

    // question id -> chosen option ('A'..'D'); a missing key means "not answered"
    private final Map<Integer, Character> answers = new HashMap<>();

    private final Label progressLabel = new Label();
    private final Label timerLabel = new Label();
    private final Button submitButton = new Button("Submit");
    private final Button leaveButton = new Button("Leave quiz");

    // timer: countdown is null when the limit is 0 or less (= no timer at all)
    private final Countdown countdown;
    private Timeline timeline;

    private Stage stage;
    private VBox questionsBox;        // all question cards (disabled when time is up)
    private Alert openConfirmation;   // the confirmation that is open right now (or null)

    private boolean ended = false;    // true after a successful save or a confirmed leave
    private boolean timeIsUp = false; // true once the time-up routine has started (runs only once)

    public QuizView(User user, String subject, List<Question> paper, Window owner, Runnable onFinished) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.paper = new ArrayList<>(Objects.requireNonNull(paper, "paper must not be null"));
        this.owner = owner;
        this.onFinished = Objects.requireNonNull(onFinished, "onFinished must not be null");

        int limit = readTimeLimit();
        this.countdown = limit > 0 ? new Countdown(limit) : null;
    }

    /** Reads quiz.time.limit.seconds. A missing or invalid value means "no timer" (0). */
    private static int readTimeLimit() {
        try {
            return ConfigLoader.getInt("quiz.time.limit.seconds");
        } catch (RuntimeException e) {
            AppLogger.warning("Config key quiz.time.limit.seconds is missing or invalid; the quiz has no timer");
            return 0;
        }
    }

    /** Opens the quiz window. */
    public void show() {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL); // the student screen behind cannot be clicked
        stage.setTitle("Quiz - " + subject);
        stage.setScene(new Scene(buildRoot(), 800, 650));

        // The window's X button: ask first. After a save or a confirmed leave we do not ask again.
        stage.setOnCloseRequest(event -> {
            if (ended) {
                return;
            }
            if (confirmLeave() && !ended) {
                ended = true;
                stopTimer();
                logLeft();
            } else {
                event.consume(); // cancel the closing
            }
        });

        // safety net: whatever closes the window, the timer must not keep running
        stage.setOnHidden(event -> stopTimer());

        updateProgress();
        stage.show();
        startTimer();
    }

    // =====================================================================
    // Layout
    // =====================================================================

    private BorderPane buildRoot() {
        // TOP: title, then (only with a timer) the time label, then the progress label
        Label title = new Label(subject + " quiz");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(20, title, spacer);
        if (countdown != null) {
            updateTimerLabel();
            top.getChildren().add(timerLabel);
        }
        top.getChildren().add(progressLabel);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10, 15, 10, 15));
        top.setStyle("-fx-background-color: #eef2f7; -fx-border-color: #c9d1dc; -fx-border-width: 0 0 1 0;");

        // CENTRE: one card per question inside a ScrollPane
        questionsBox = new VBox(12);
        questionsBox.setPadding(new Insets(15));
        int number = 1;
        for (Question q : paper) {
            questionsBox.getChildren().add(buildCard(number, q));
            number++;
        }

        ScrollPane scroll = new ScrollPane(questionsBox);
        scroll.setFitToWidth(true);                                  // no sideways scrolling
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // BOTTOM: buttons
        submitButton.setStyle("-fx-font-weight: bold;");
        submitButton.setOnAction(e -> handleSubmit());
        leaveButton.setOnAction(e -> handleLeave());

        HBox bottom = new HBox(10, submitButton, leaveButton);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(10, 15, 10, 15));
        bottom.setStyle("-fx-background-color: #eef2f7; -fx-border-color: #c9d1dc; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(scroll);
        root.setBottom(bottom);
        return root;
    }

    /** One question: text, difficulty tag and four RadioButtons in ONE ToggleGroup. */
    private VBox buildCard(int number, Question q) {
        Label text = new Label("Q" + number + ". " + q.getQuestionText());
        text.setWrapText(true);
        text.setMinHeight(Region.USE_PREF_SIZE); // show every wrapped line
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label difficulty = new Label("Difficulty: " + q.getDifficulty());
        difficulty.setStyle("-fx-text-fill: #777777; -fx-font-size: 11px;");

        VBox card = new VBox(8, text, difficulty);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 6;"
                + " -fx-border-color: #c9d1dc; -fx-border-radius: 6;");

        ToggleGroup group = new ToggleGroup(); // only one option of this question can be chosen
        char[] letters = {'A', 'B', 'C', 'D'};
        for (char letter : letters) {
            RadioButton option = new RadioButton(letter + ". " + q.getOptionText(letter));
            option.setToggleGroup(group);
            option.setUserData(Character.valueOf(letter)); // remember the letter inside the button
            option.setWrapText(true);
            option.setMaxWidth(Double.MAX_VALUE);
            option.setMinHeight(Region.USE_PREF_SIZE);
            card.getChildren().add(option);
        }

        // when the student picks an option, store the letter in the HashMap
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                answers.put(q.getQuestionId(), (Character) newToggle.getUserData());
                updateProgress();
            }
        });
        return card;
    }

    private void updateProgress() {
        progressLabel.setText("Answered " + answers.size() + " / " + paper.size());
    }

    // =====================================================================
    // Timer
    // =====================================================================

    private void startTimer() {
        if (countdown == null) {
            return; // no limit configured: no Timeline at all
        }
        // one KeyFrame every second, repeated forever until we stop it
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> onTick()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    /** Runs once per second. */
    private void onTick() {
        countdown.tick();
        updateTimerLabel();

        if (countdown.isFinished()) {
            stopTimer();
            // Alert.showAndWait() throws IllegalStateException when it is called directly inside an
            // animation handler. Platform.runLater postpones the work until the animation step is over.
            Platform.runLater(this::timeUp);
        }
    }

    private void updateTimerLabel() {
        timerLabel.setText("Time left: " + countdown.getDisplayText());
        if (countdown.isWarning()) {
            timerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: red;");
        } else {
            timerLabel.setStyle("-fx-font-size: 14px;");
        }
    }

    /** The time-up routine. It runs only once and does nothing if the quiz has already ended. */
    private void timeUp() {
        if (timeIsUp || ended) {
            return;
        }
        timeIsUp = true;

        // 1. close a confirmation that is open right now. Every confirmation treats anything
        //    other than OK as Cancel, so closing it is the same as pressing Cancel.
        if (openConfirmation != null) {
            openConfirmation.close();
        }

        // 2. answers can no longer be changed
        questionsBox.setDisable(true);

        AppLogger.info(user.getUsername() + " ran out of time in " + subject + "; auto-submitting");
        showAlert(Alert.AlertType.INFORMATION, "Time is up",
                "Time is up! Your answers are being submitted.");

        // 3. same as the Submit button, but without the "unanswered questions" question
        submit(false);
    }

    // =====================================================================
    // Submit and leave
    // =====================================================================

    private void handleSubmit() {
        // after time is up we never ask about unanswered questions again
        submit(!timeIsUp);
    }

    private void submit(boolean askAboutUnanswered) {
        if (ended) {
            return; // already saved or left: never save twice
        }

        if (askAboutUnanswered) {
            int unanswered = QuizService.countUnanswered(paper, answers);
            if (unanswered > 0) {
                boolean go = confirm("Submit quiz", "You have not answered " + unanswered
                        + " question(s). They will count as wrong. Submit anyway?");
                if (!go || ended) {
                    return; // Cancel (or time ran out meanwhile): stay on the quiz
                }
            }
        }

        submitButton.setDisable(true); // guard against a double click while saving
        try {
            Result result = quizService.submitQuiz(user.getUserId(), subject, paper, answers);
            ended = true;
            stopTimer();

            // only the score is shown, never the correct answers
            showAlert(Alert.AlertType.INFORMATION, "Quiz finished",
                    "You scored " + result.getScore() + " / " + result.getTotal()
                            + " (" + result.getPercentage() + "%)");

            stage.close();
            onFinished.run(); // the student screen reloads its lists
        } catch (IllegalStateException ex) {
            // the service has already logged the SQLException; the quiz (and the timer) stay open
            showAlert(Alert.AlertType.ERROR, "Database error",
                    "Your result could not be saved. The quiz is still open, please try Submit again.");
            if (!ended) {
                submitButton.setDisable(false);
            }
        }
    }

    /** "Leave quiz" button. stage.close() does not trigger setOnCloseRequest, so we ask here. */
    private void handleLeave() {
        if (ended) {
            return;
        }
        if (confirmLeave() && !ended) {
            ended = true;
            stopTimer();
            logLeft();
            stage.close();
        }
    }

    private boolean confirmLeave() {
        return confirm("Leave quiz",
                "Leave the quiz? Your answers will be lost and no result will be saved.");
    }

    private void logLeft() {
        AppLogger.info(user.getUsername() + " left the quiz without submitting");
    }

    // =====================================================================
    // Alert helpers (the quiz window is always the owner)
    // =====================================================================

    /** Only OK counts as yes. Cancel, or a confirmation closed by the timer, counts as no. */
    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(stage);

        openConfirmation = alert; // so the time-up routine can close it
        Optional<ButtonType> answer;
        try {
            answer = alert.showAndWait();
        } finally {
            if (openConfirmation == alert) {
                openConfirmation = null;
            }
        }
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}