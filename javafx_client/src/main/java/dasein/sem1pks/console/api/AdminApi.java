package dasein.sem1pks.console.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class AdminApi {
    private final HttpTransport http;

    AdminApi(HttpTransport http) { this.http = http; }

    public Path export(Path destination) throws IOException {
        byte[] bytes = http.request("GET", "/api/admin/export", null, true, true);
        return Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}
