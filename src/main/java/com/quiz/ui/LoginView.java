package com.quiz.ui;

import com.quiz.Main;
import com.quiz.exception.DuplicateUserException;
import com.quiz.exception.InvalidLoginException;
import com.quiz.model.Student;
import com.quiz.model.User;
import com.quiz.service.AuthService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login / Register screen. One card, two modes. The form is swapped inside
 * the same window, so no second window is ever opened.
 */
public class LoginView {

    private final AuthService authService = new AuthService();

    private final StackPane root = new StackPane();
    private final Label subtitle = new Label();
    private final VBox formArea = new VBox();   // holds either the login form or the register form

    // Login fields
    private final TextField loginUsername = new TextField();
    private final PasswordField loginPassword = new PasswordField();

    // Register fields
    private final TextField regFullName = new TextField();
    private final TextField regUsername = new TextField();
    private final PasswordField regPassword = new PasswordField();
    private final PasswordField regConfirm = new PasswordField();

    private final GridPane loginForm;
    private final GridPane registerForm;

    public LoginView() {
        loginForm = buildLoginForm();
        registerForm = buildRegisterForm();

        Label title = new Label("QuizForge Lite");
        title.getStyleClass().add("auth-title");
        subtitle.getStyleClass().add("auth-subtitle");
        Label mark = new Label("Q");
        mark.getStyleClass().add("auth-brand-mark");
        Label tagline = new Label("LEARN. PRACTICE. PROGRESS.");
        tagline.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #8290a9;");
        VBox brandCopy = new VBox(3, title, tagline);
        HBox brand = new HBox(12, mark, brandCopy);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label portalNote = new Label("One secure portal for students and administrators.");
        portalNote.getStyleClass().add("auth-note");
        portalNote.setWrapText(true);

        VBox card = new VBox(16, brand, subtitle, portalNote, formArea);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(32));
        card.setMaxWidth(410);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().add("auth-card");

        root.getStyleClass().add("auth-root");
        root.setPadding(new Insets(28));
        root.getChildren().add(card);

        showLoginMode();
    }

    public StackPane getRoot() {
        return root;
    }

    // ------------------------------------------------------------ mode switching

    private void showLoginMode() {
        subtitle.setText("Sign in to continue");
        formArea.getChildren().setAll(loginForm);
        loginUsername.requestFocus();
    }

    private void showRegisterMode() {
        subtitle.setText("Create a student account");
        formArea.getChildren().setAll(registerForm);
        regFullName.requestFocus();
    }

    // ------------------------------------------------------------ building the forms

    private GridPane buildLoginForm() {
        GridPane grid = newGrid();

        styleField(loginUsername, "Username");
        styleField(loginPassword, "Password");

        Button loginButton = primaryButton("Login");
        loginButton.setOnAction(e -> handleLogin());

        // Pressing Enter inside either field also logs in.
        loginUsername.setOnAction(e -> handleLogin());
        loginPassword.setOnAction(e -> handleLogin());

        Button toRegister = linkButton("New student? Create an account");
        toRegister.setOnAction(e -> showRegisterMode());

        grid.add(new Label("Username"), 0, 0);
        grid.add(loginUsername, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(loginPassword, 1, 1);
        grid.add(loginButton, 0, 2, 2, 1);
        grid.add(centred(toRegister), 0, 3, 2, 1);
        return grid;
    }

    private GridPane buildRegisterForm() {
        GridPane grid = newGrid();

        styleField(regFullName, "Full name");
        styleField(regUsername, "3-20 letters, digits, _");
        styleField(regPassword, "At least 6 characters");
        styleField(regConfirm, "Repeat password");

        Button registerButton = primaryButton("Register");
        registerButton.setOnAction(e -> handleRegister());
        regConfirm.setOnAction(e -> handleRegister());   // Enter on the last field registers

        Button toLogin = linkButton("Back to login");
        toLogin.setOnAction(e -> showLoginMode());

        grid.add(new Label("Full name"), 0, 0);
        grid.add(regFullName, 1, 0);
        grid.add(new Label("Username"), 0, 1);
        grid.add(regUsername, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(regPassword, 1, 2);
        grid.add(new Label("Confirm password"), 0, 3);
        grid.add(regConfirm, 1, 3);
        grid.add(registerButton, 0, 4, 2, 1);
        grid.add(centred(toLogin), 0, 5, 2, 1);
        return grid;
    }

    // ------------------------------------------------------------ button actions

    private void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing details",
                    "Please enter both username and password.");
            return;
        }

        try {
            User user = authService.login(username, password);
            loginPassword.clear();          // never keep the password in the field
            Main.showDashboard(user);       // switch to the admin or student screen
        } catch (InvalidLoginException e) {
            showAlert(Alert.AlertType.ERROR, "Login failed", e.getMessage());
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void handleRegister() {
        String fullName = regFullName.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String confirm = regConfirm.getText();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing details", "Please fill in every field.");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Passwords do not match",
                    "Password and confirm password must be the same.");
            return;
        }

        try {
            Student student = authService.register(username, password, fullName);
            showAlert(Alert.AlertType.INFORMATION, "Registration successful",
                    "Account created for " + student.getFullName() + ". You can log in now.");

            clearRegisterFields();
            loginUsername.setText(student.getUsername());   // prefill the username
            loginPassword.clear();
            showLoginMode();
            Platform.runLater(loginPassword::requestFocus);  // cursor goes to the password box
        } catch (DuplicateUserException e) {
            showAlert(Alert.AlertType.ERROR, "Registration failed", e.getMessage());
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", e.getMessage());
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    private void clearRegisterFields() {
        regFullName.clear();
        regUsername.clear();
        regPassword.clear();
        regConfirm.clear();
    }

    // ------------------------------------------------------------ small UI helpers

    private static void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("QuizForge Lite");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Empty grid with spacing and two columns: labels (fixed) and fields (stretch). */
    private static GridPane newGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return grid;
    }

    private static void styleField(TextField field, String prompt) {
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);   // lets the field stretch to the column width
    }

    private static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private static Button linkButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("auth-link-button");
        return button;
    }

    private static HBox centred(Button button) {
        HBox box = new HBox(button);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
