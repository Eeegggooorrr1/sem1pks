package dasein.sem1pks.console.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dasein.sem1pks.console.session.TokenStore;

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

public final class HttpTransport {
    private final URI baseUri;
    private final TokenStore tokens;
    private final HttpClient http;
    private final ObjectMapper json = JsonMapper.builder().build();

    public HttpTransport(String baseUrl, TokenStore tokens) {
        this.baseUri = URI.create(baseUrl.replaceAll("/+$", ""));
        if (!List.of("http", "https").contains(baseUri.getScheme()) || baseUri.getHost() == null
                || baseUri.getUserInfo() != null || baseUri.getQuery() != null || baseUri.getFragment() != null) {
            throw new IllegalArgumentException("API_URL должен быть HTTP(S) URL без логина, query и fragment");
        }
        this.tokens = tokens;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public String baseUrl() {
        return baseUri.toString();
    }

    byte[] request(String method, String path, Object body, boolean authenticated, boolean binary) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUri + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", binary ? "application/zip" : "application/json");
            if (authenticated && tokens.get() != null) {
                builder.header("Authorization", "Bearer " + tokens.get());
            }
            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                builder.header("Content-Type", "application/json");
                publisher = HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body));
            }
            HttpResponse<byte[]> response = http.send(builder.method(method, publisher).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 300) {
                if (response.statusCode() == 401 && authenticated) {
                    tokens.clear();
                }
                throw new ApiException(response.statusCode(), errorMessage(response));
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Запрос прерван", e);
        } catch (IOException e) {
            throw new ApiException("Не удалось выполнить запрос к API: " + e.getMessage(), e);
        }
    }

    private String errorMessage(HttpResponse<byte[]> response) {
        String message = "HTTP " + response.statusCode();
        try {
            var error = json.readTree(response.body());
            if (error != null && error.hasNonNull("message")) {
                message += ": " + error.get("message").asText();
            }
            if (error != null && error.hasNonNull("details")) {
                message += " " + error.get("details");
            }
        } catch (IOException ignored) {
            // A non-JSON error response still produces a useful HTTP status.
        }
        return message;
    }

    <T> T decode(byte[] bytes, Class<T> type) {
        if (bytes.length == 0) {
            return null;
        }
        try {
            return json.readValue(bytes, type);
        } catch (IOException e) {
            throw new ApiException("Некорректный ответ API", e);
        }
    }

    <T> List<T> decodeList(byte[] bytes, Class<T> type) {
        try {
            return json.readValue(bytes, json.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new ApiException("Некорректный список в ответе API", e);
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
                query.append(query.isEmpty() ? '?' : '&').append(key).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        });
        return query.toString();
    }
}
