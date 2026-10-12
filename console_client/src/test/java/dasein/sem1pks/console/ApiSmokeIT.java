package dasein.sem1pks.console;

import dasein.sem1pks.console.api.ApiClient;
import dasein.sem1pks.console.dto.account.*;
import dasein.sem1pks.console.dto.listing.*;
import dasein.sem1pks.console.dto.order.*;
import dasein.sem1pks.console.dto.statistics.*;
import dasein.sem1pks.console.session.TokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ApiSmokeIT {
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }

    @Test
    void completeHttpWorkflow(@TempDir Path directory) throws Exception {
        String url = required("SMOKE_API_URL");
        ApiClient seller = new ApiClient(url, new TokenStore());
        ApiClient buyer = new ApiClient(url, new TokenStore());
        ApiClient admin = new ApiClient(url, new TokenStore());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        seller.accounts().register(new RegisterRequest("Smoke seller", "seller-" + suffix + "@example.com", "Smoke-test-123"));
        buyer.accounts().register(new RegisterRequest("Smoke buyer", "buyer-" + suffix + "@example.com", "Smoke-test-123"));
        ListingResponse listing = seller.listings().create(new ListingCreateRequest(
                "Smoke " + suffix, "Проверка Java-клиента", new BigDecimal("123.45"), "BOOKS"));
        assertEquals(listing.id(), seller.listings().search("Smoke " + suffix, null, "CREATED_AT", "DESC").get(0).id());
        assertEquals(listing.id(), seller.listings().mine().get(0).id());
        OrderResponse order = buyer.orders().create(listing.id());
        assertEquals(order.id(), seller.orders().find("INCOMING").get(0).id());
        assertEquals(order.id(), buyer.orders().find("OUTGOING").get(0).id());
        assertEquals("CONFIRMED", seller.orders().changeStatus(order.id(), "confirm").status());
        assertEquals("COMPLETED", buyer.orders().changeStatus(order.id(), "complete").status());
        assertEquals("SOLD", seller.listings().changeStatus(listing.id(), "sold").status());
        admin.accounts().login(new LoginRequest(
                required("BOOTSTRAP_ADMIN_EMAIL"),
                required("BOOTSTRAP_ADMIN_PASSWORD")));
        assertTrue(admin.statistics().get().ordersCount() > 0);
        Path export = admin.admin().export(directory.resolve("export.zip"));
        try (ZipFile zip = new ZipFile(export.toFile())) {
            assertEquals(Set.of("users.csv", "listings.csv", "orders.csv"),
                    zip.stream().map(entry -> entry.getName()).collect(Collectors.toSet()));
        }
        assertNull(admin.listings().changeStatus(listing.id(), "closed/admin"));
        assertTrue(seller.listings().search("Smoke " + suffix, null, "CREATED_AT", "DESC").isEmpty());
    }
}
