package com.quiz;

import com.quiz.model.User;
import com.quiz.ui.AdminView;
import com.quiz.ui.LoginView;
import com.quiz.ui.StudentView;
import com.quiz.util.AppLogger;
import com.quiz.util.ConfigLoader;
import com.quiz.util.DBConnection;
import com.quiz.util.DataSeeder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** Entry point. Owns the one and only window (Stage) and switches the screens inside it. */
public class Main extends Application {

    private static Stage primaryStage;
    private static User currentUser;
    private static String startupError;   // filled in init() if something goes wrong

    /** Runs BEFORE the window exists: prepare config, database and demo data. */
    @Override
    public void init() {
        try {
            ConfigLoader.load();
            AppLogger.info("Config loaded. Database file: " + ConfigLoader.getString("db.path"));

            DBConnection.initSchema();     // create tables if they are missing
            DataSeeder.seedIfEmpty();      // demo admin, student and 30 questions (only if empty)
            AppLogger.info("QuizForge Lite initialised. Tables found: " + DBConnection.countTables());
        } catch (Exception e) {
            // Do not crash here: remember the problem and show it in start().
            startupError = e.getMessage() == null ? e.toString() : e.getMessage();
            logError("Startup failed", e);
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        if (startupError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("QuizForge Lite");
            alert.setHeaderText("The application could not start");
            alert.setContentText(startupError);
            alert.showAndWait();
            Platform.exit();
            return;
        }

        showLogin();
    }

    @Override
    public void stop() {
        logInfo("Application shutting down");
    }

    // ------------------------------------------------------------ screen switching

    /** Opens the admin or student screen, depending on the user's role. */
    public static void showDashboard(User user) {
        currentUser = user;
        Parent root;
        if ("ADMIN".equals(user.getRole())) {
            root = new AdminView(user).getRoot();
        } else {
            root = new StudentView(user).getRoot();
        }
        logInfo("Dashboard opened for " + user.getUsername() + " (" + user.getRole() + ")");
        show(root, user.getDashboardTitle(), 1000, 650);
    }

    /** Forgets the current user and goes back to the login screen. */
    public static void logout() {
        String name = currentUser == null ? "unknown" : currentUser.getUsername();
        logInfo("Logout: " + name);
        currentUser = null;
        showLogin();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    private static void showLogin() {
        show(new LoginView().getRoot(), "QuizForge Lite", 480, 560);
    }

    /** Puts a new Scene in the same Stage, resizes the window and centres it. */
    private static void show(Parent root, String title, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(Main.class.getResource("/css/dashboard.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
    }

    // ---- small logging helpers that call your real AppLogger ----
    private static void logInfo(String message) {
        AppLogger.info(message);
    }

    private static void logError(String message, Exception e) {
        AppLogger.severe(message, e);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
