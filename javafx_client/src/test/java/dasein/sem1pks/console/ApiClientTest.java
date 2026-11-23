package dasein.sem1pks.console;

import com.sun.net.httpserver.HttpServer;
import dasein.sem1pks.console.api.ApiClient;
import dasein.sem1pks.console.api.ApiException;
import dasein.sem1pks.console.dto.account.LoginRequest;
import dasein.sem1pks.console.session.TokenStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {
    private HttpServer server;
    private TokenStore tokens;
    private ApiClient api;
    private volatile String authorization;
    private volatile String query;
    private volatile String requestBody;
    private volatile String responseBody;
    private volatile String redirect;
    private volatile int status;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        responseBody = "[]";
        status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            query = exchange.getRequestURI().getRawQuery();
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            if (redirect != null) {
                exchange.getResponseHeaders().add("Location", redirect);
            }
            exchange.sendResponseHeaders(status, status == 204 ? -1 : body.length);
            if (status != 204) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        tokens = new TokenStore();
        api = new ApiClient("http://127.0.0.1:" + server.getAddress().getPort(), tokens);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void loginStoresTokenWithoutSendingOldCredentials() {
        tokens.set("old-token");
        responseBody = """
                {"accessToken":"new-token","account":{"id":1,"email":"a@b.com","username":"alice","role":"USER"}}
                """;
        assertEquals(1, api.accounts().login(new LoginRequest("a@b.com", "password")).id());
        assertEquals("new-token", tokens.get());
        assertNull(authorization);
        assertTrue(requestBody.contains("\"password\":\"password\""));
        assertFalse(new LoginRequest("a@b.com", "secret").toString().contains("secret"));
    }

    @Test
    void encodesFiltersAndSendsBearer() {
        tokens.set("token");
        assertTrue(api.listings().search("a&b", "BOOKS", "PRICE", "ASC").isEmpty());
        assertTrue(query.contains("prefix=a%26b"));
        assertTrue(query.contains("sortBy=PRICE"));
        assertEquals("Bearer token", authorization);
    }

    @Test
    void unauthorizedClearsTokenAndIncludesValidationDetails() {
        tokens.set("expired");
        status = 401;
        responseBody = "{\"message\":\"expired\",\"details\":{\"token\":[\"invalid\"]}}";
        ApiException error = assertThrows(ApiException.class, () -> api.listings().mine());
        assertEquals(401, error.status());
        assertTrue(error.getMessage().contains("expired"));
        assertTrue(error.getMessage().contains("invalid"));
        assertNull(tokens.get());
    }

    @Test
    void handlesNoContentAndDoesNotOverwriteExport(@TempDir Path directory) throws IOException {
        status = 204;
        assertNull(api.orders().changeStatus(1, "cancel"));
        status = 200;
        responseBody = "zip bytes";
        Path file = directory.resolve("export.zip");
        api.admin().export(file);
        assertEquals("zip bytes", Files.readString(file));
        assertThrows(FileAlreadyExistsException.class, () -> api.admin().export(file));
        assertEquals("zip bytes", Files.readString(file));
    }

    @Test
    void rejectsRedirectWithoutForwardingCredentials() {
        tokens.set("secret-token");
        status = 302;
        redirect = api.baseUrl() + "/other";
        assertEquals(302, assertThrows(ApiException.class, () -> api.listings().mine()).status());
        assertEquals(1, calls.get());
    }

    @Test
    void reportsMalformedJsonAndClearsSessionOnLogout() {
        responseBody = "not json";
        assertThrows(ApiException.class, () -> api.listings().mine());
        tokens.set("token");
        api.accounts().logout();
        assertNull(tokens.get());
    }
}
