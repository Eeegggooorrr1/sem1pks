package dasein.sem1pks.javafx.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dasein.sem1pks.javafx.session.Session;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class HttpTransport {
    private final URI baseUri;
    private final Session session;
    private final HttpClient http;
    private final ObjectMapper json = new JsonMapper();

    HttpTransport(String baseUrl, Session session) {
        baseUri = URI.create(baseUrl.replaceAll("/+$", ""));
        if (!List.of("http", "https").contains(baseUri.getScheme()) || baseUri.getHost() == null
                || baseUri.getUserInfo() != null || baseUri.getQuery() != null || baseUri.getFragment() != null) {
            throw new IllegalArgumentException("Адрес API должен быть корректным HTTP(S) URL");
        }
        this.session = session;
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    byte[] request(String method, String path, Object body, boolean authenticated, boolean binary) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUri + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", binary ? "application/zip" : "application/json");

            if (authenticated && session.token() != null) {
                builder.header("Authorization", "Bearer " + session.token());
            }

            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                builder.header("Content-Type", "application/json");
                publisher = HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body));
            }

            HttpResponse<byte[]> response = http.send(
                    builder.method(method, publisher).build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() >= 300) {
                if (response.statusCode() == 401 && authenticated) {
                    session.clear();
                }
                throw new ApiException(response.statusCode(), readableError(response.statusCode(), response.body()));
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException("Запрос был прерван", exception);
        } catch (IOException exception) {
            throw new ApiException("Не удалось связаться с сервером. Проверьте, что backend запущен.", exception);
        }
    }

    <T> T decode(byte[] bytes, Class<T> type) {
        if (bytes.length == 0) {
            return null;
        }
        try {
            return json.readValue(bytes, type);
        } catch (IOException exception) {
            throw new ApiException("Сервер вернул ответ в неизвестном формате", exception);
        }
    }

    <T> List<T> decodeList(byte[] bytes, Class<T> type) {
        try {
            return json.readValue(bytes, json.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException exception) {
            throw new ApiException("Сервер вернул список в неизвестном формате", exception);
        }
    }

    private String readableError(int status, byte[] body) {
        if (status >= 500) {
            return "Ошибка на сервере. Попробуйте ещё раз позже.";
        }

        String serverMessage = extractServerMessage(body);
        if (serverMessage != null) {
            return serverMessage;
        }

        return switch (status) {
            case 400 -> "Проверьте введённые данные";
            case 401 -> "Неверные данные для входа или сессия завершилась";
            case 403 -> "У вас недостаточно прав для этого действия";
            case 404 -> "Запрошенные данные не найдены";
            case 409 -> "Действие нельзя выполнить в текущем состоянии";
            default -> "Не удалось выполнить запрос";
        };
    }

    private String extractServerMessage(byte[] body) {
        try {
            JsonNode error = json.readTree(body);
            if (error == null) {
                return null;
            }
            String message = error.hasNonNull("message") ? error.get("message").asText() : null;
            JsonNode details = error.get("details");
            if (details != null && !details.isNull() && !details.isEmpty()) {
                String detailText = details.fields().next().getValue().get(0).asText();
                return message == null ? detailText : message + ": " + detailText;
            }
            return message;
        } catch (Exception ignored) {
            return null;
        }
    }

    static long positive(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
        return id;
    }

    static String queryString(Map<String, String> values) {
        StringBuilder query = new StringBuilder();
        values.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                query.append(query.isEmpty() ? '?' : '&')
                        .append(key)
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return query.toString();
    }
}
