package dasein.sem1pks.javafx;

import dasein.sem1pks.javafx.api.ApiClient;
import dasein.sem1pks.javafx.session.Session;
import dasein.sem1pks.javafx.ui.AppView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        String apiUrl = System.getenv().getOrDefault("API_URL", "http://localhost:8080");
        Session session = new Session();
        ApiClient api = new ApiClient(apiUrl, session);

        Scene scene = new Scene(new AppView(api, session), 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Сервис объявлений");
        stage.setMinWidth(850);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
