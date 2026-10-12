package dasein.sem1pks.javafx.ui;

import javafx.fxml.FXMLLoader;

import java.io.IOException;

final class FxmlView {
    private FxmlView() {
    }

    static void load(Object view, String resource) {
        FXMLLoader loader = new FXMLLoader(FxmlView.class.getResource(resource));
        loader.setRoot(view);
        loader.setController(view);
        try {
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось загрузить интерфейс " + resource, exception);
        }
    }

    static <T> T load(String resource, Object controller) {
        FXMLLoader loader = new FXMLLoader(FxmlView.class.getResource(resource));
        loader.setController(controller);
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось загрузить интерфейс " + resource, exception);
        }
    }
}
