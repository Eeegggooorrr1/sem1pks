package dasein.sem1pks.console.config;

import java.nio.file.Path;

public record ClientConfig(String apiUrl, Path exportDirectory) {
    public static ClientConfig fromEnvironment() {
        return new ClientConfig(required("API_URL"), Path.of(required("EXPORT_DIR")));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задана переменная " + name);
        }
        return value;
    }
}
