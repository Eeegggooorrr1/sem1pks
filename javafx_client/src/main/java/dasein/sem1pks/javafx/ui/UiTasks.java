package dasein.sem1pks.javafx.ui;

import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class UiTasks {
    private UiTasks() {
    }

    static <T> void run(Node busyNode, Supplier<T> operation, Consumer<T> success, Consumer<Throwable> failure) {
        busyNode.setDisable(true);
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return operation.get();
            }
        };
        task.setOnSucceeded(event -> {
            busyNode.setDisable(false);
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busyNode.setDisable(false);
            failure.accept(task.getException());
        });

        Thread thread = new Thread(task, "javafx-api-request");
        thread.setDaemon(true);
        thread.start();
    }

    static void run(Node busyNode, Runnable operation, Runnable success, Consumer<Throwable> failure) {
        run(busyNode, () -> {
            operation.run();
            return null;
        }, ignored -> success.run(), failure);
    }
}
