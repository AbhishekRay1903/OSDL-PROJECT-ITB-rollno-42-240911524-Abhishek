package com.osdl.app;

import com.osdl.db.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException, SQLException {
        Database.initializeSchema();

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(App.class.getResource("/fxml/main.fxml")));
        Scene scene = new Scene(loader.load(), 960, 640);
        String css = Objects.requireNonNull(App.class.getResource("/css/app.css")).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Hotel Management — JavaFX + JDBC");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
