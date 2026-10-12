package dasein.sem1pks.javafx.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AdminApi {
    private final HttpTransport http;

    AdminApi(HttpTransport http) {
        this.http = http;
    }

    public Path export(Path destination) {
        byte[] bytes = http.request("GET", "/api/admin/export", null, true, true);
        try {
            return Files.write(destination, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException("Не удалось сохранить архив: " + exception.getMessage(), exception);
        }
    }
}
