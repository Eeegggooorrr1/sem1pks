package dasein.sem1pks;

import dasein.sem1pks.bootstrap.BootstrapProperties;
import dasein.sem1pks.bootstrap.BootstrapService;
import dasein.sem1pks.domain.*;
import dasein.sem1pks.dto.request.*;
import dasein.sem1pks.exception.conflict.OrderCreateConflictException;
import dasein.sem1pks.exception.conflict.OrderStateConflictException;
import dasein.sem1pks.exception.forbidden.AccountBlockedException;
import dasein.sem1pks.exception.forbidden.ListingAccessDeniedException;
import dasein.sem1pks.exception.unauthorized.InvalidCredentialsException;
import dasein.sem1pks.repository.*;
import dasein.sem1pks.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ApplicationContract {
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired OrderRepository orders;
    @Autowired StatisticsRepository statistics;
    @Autowired AccountService accounts;
    @Autowired ListingService listingService;
    @Autowired OrderService orderService;
    @Autowired BootstrapService bootstrap;
    @Autowired BootstrapProperties bootstrapProperties;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtService jwt;
    @Autowired UserAdminService userAdmin;

    @BeforeEach
    void reset() {
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).endsWith("_test");
        jdbc.execute(
                "TRUNCATE orders,listings,users,bootstrap_history,audit_events RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE categories SET enabled=TRUE");
    }

    @Test
    void refreshRotationLoginRevocationAndLogout() throws Exception {
        var first = accounts.createAccount("Alice", "alice@example.com", "Password-123");
        String stored = jdbc.queryForObject("SELECT token_hash FROM refresh_sessions WHERE user_id=?",
                String.class, first.account().id());
        assertThat(stored).hasSize(64).isNotEqualTo(first.refreshToken());
        var second = accounts.refresh(first.refreshToken());
        assertThatThrownBy(() -> accounts.refresh(first.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
        var login = accounts.login("alice@example.com", "Password-123");
        assertThatThrownBy(() -> accounts.refresh(second.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
        mvc.perform(post("/api/users/logout")
                            .contentType("application/json")
                            .content("{\"refreshToken\":\"" + login.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM refresh_sessions WHERE user_id=?",
                           Boolean.class, first.account().id()))
                .isTrue();
        assertThatThrownBy(() -> accounts.refresh(login.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void passwordChangeRevokesSessionAndMeReturnsNoPassword() throws Exception {
        var account = accounts.createAccount("Alice", "alice@example.com", "Password-123");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + account.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        accounts.changePassword(account.account().id(), "Password-123", "New-password-123");
        assertThatThrownBy(() -> accounts.refresh(account.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> accounts.login("alice@example.com", "Password-123"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(accounts.login("alice@example.com", "New-password-123").accessToken()).isNotBlank();
    }

    @Test
    void adminCreatesAndDisablesUsers() throws Exception {
        var admin = user("Admin");
        admin.setRole(UserRole.ADMIN);
        users.save(admin);
        var created = userAdmin.create(admin.getId(),
                new AdminUserRequest("Reader", "reader@example.com", UserRole.USER, true), "Password-123");
        var session = accounts.login(created.email(), "Password-123");
        userAdmin.update(admin.getId(), created.id(),
                new AdminUserRequest("Reader", "reader@example.com", UserRole.USER, false));
        assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM refresh_sessions WHERE user_id=?",
                           Boolean.class, created.id()))
                .isTrue();
        assertThatThrownBy(() -> accounts.refresh(session.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
        mvc.perform(post("/api/users/login")
                            .contentType("application/json")
                            .content("{\"email\":\"reader@example.com\",\"password\":\"Password-123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_BLOCKED"));
        mvc.perform(post("/api/users/login")
                            .contentType("application/json")
                            .content("{\"email\":\"reader@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/users?size=101").header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotChangeOwnRoleOrDisableOwnAccount() throws Exception {
        var admin = user("Admin");
        admin.setRole(UserRole.ADMIN);
        users.save(admin);
        String url = "/api/admin/users/" + admin.getId();
        String authorization = "Bearer " + token(admin);
        String request = """
                {"username":"Renamed admin","email":"%s","role":"%s","active":%b}
                """;

        mvc.perform(put(url).header("Authorization", authorization).contentType("application/json")
                        .content(request.formatted(admin.getEmail(), UserRole.USER, true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_SELF_ACCESS_CHANGE"));
        mvc.perform(put(url).header("Authorization", authorization).contentType("application/json")
                        .content(request.formatted(admin.getEmail(), UserRole.ADMIN, false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_SELF_ACCESS_CHANGE"));

        var saved = users.findById(admin.getId()).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.isBlocked()).isFalse();
        mvc.perform(put(url).header("Authorization", authorization).contentType("application/json")
                        .content(request.formatted(admin.getEmail(), UserRole.ADMIN, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Renamed admin"));

        var otherAdmin = user("OtherAdmin");
        otherAdmin.setRole(UserRole.ADMIN);
        users.save(otherAdmin);
        mvc.perform(put(url).header("Authorization", "Bearer " + token(otherAdmin)).contentType("application/json")
                        .content(request.formatted(admin.getEmail(), UserRole.USER, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void disabledUserCannotRefreshAndAnonymousCannotWrite() throws Exception {
        var account = accounts.createAccount("Alice", "alice@example.com", "Password-123");
        var user = users.findById(account.account().id()).orElseThrow();
        user.setBlocked(true);
        users.save(user);
        assertThatThrownBy(() -> accounts.refresh(account.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> accounts.login("alice@example.com", "Password-123"))
                .isInstanceOf(AccountBlockedException.class);
        user.setBlocked(false);
        users.save(user);
        mvc.perform(post("/api/listings").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + token(user)))
                .andExpect(status().isForbidden());
        user.setRole(UserRole.ADMIN);
        users.save(user);
        mvc.perform(get("/api/admin/audit")
                            .header("Authorization", "Bearer " + token(user))
                            .header("X-Correlation-ID", "test-request"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "test-request"))
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(get("/api/admin/audit")
                            .header("Authorization", "Bearer " + token(user))
                            .header("X-Correlation-ID", "invalid id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID",
                        matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
    }

    User user(String name) {
        var response = accounts.createAccount(name, name + "@example.com", "Password-123");
        return users.findById(response.account().id()).orElseThrow();
    }
    UUID listing(User seller, String title, int price) {
        return listingService
                .createListing(
                        seller.getId(), title, "Описание", BigDecimal.valueOf(price), ListingCategory.BOOKS)
                .id();
    }
    String token(User user) {
        return jwt.generateToken(user.getId(), user.getEmail(), user.getRole().name(), user.isBlocked());
    }

    @Test
    void readinessIsPublicButOtherActuatorEndpointsAreProtected() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationLoginAndDuplicateEmail() throws Exception {
        var registered = accounts.createAccount("Alice", " Alice@Example.com ", "Password-123");
        assertThat(accounts.login("ALICE@example.com", "Password-123").account().id())
                .isEqualTo(registered.account().id());
        assertThat(users.findByEmail("alice@example.com").orElseThrow().getPasswordHash())
                .doesNotContain("Password-123");
        mvc.perform(
                   post("/api/users/register")
                           .contentType("application/json")
                           .content(
                                   "{\"username\":\"Alice\",\"email\":\"alice@example.com\",\"password\":\"Password-123\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/users/login")
                            .contentType("application/json")
                            .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountDtoValidatesNormalizedNameAndPasswordLength() throws Exception {
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {
            "username" : " a ", "email" : "short@example.com", "password" : "Password-123"}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.username").exists());
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {
            "username" : "Alice", "email" : "long@example.com", "password" : "%s"}
            """.formatted("a".repeat(73))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.password").exists());
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {
            "username" : " Alice ", "email" : "valid@example.com", "password" : "%s"}
            """.formatted("я".repeat(36))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.account.username").value("Alice"));
    }

    @Test
    void listingStatusesArePersistedAndOwnershipChecked() throws Exception {
        User seller = user("seller"), other = user("other");
        UUID id = listing(seller, "Book", 100);
        assertThat(listingService.findMyListings(seller.getId()))
                .singleElement()
                .satisfies(l -> assertThat(l.ownerId()).isEqualTo(seller.getId()));
        assertThatThrownBy(() -> listingService.markAsSold(other.getId(), id))
                .isInstanceOf(ListingAccessDeniedException.class);
        listingService.markAsSold(seller.getId(), id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.SOLD);
        listingService.markAsActive(seller.getId(), id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.ACTIVE);
        listingService.markAsClosed(seller.getId(), id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.CLOSED);
    }

    @Test
    void searchFiltersSortsAndEscapesLiteralPrefix() {
        User seller = user("seller");
        UUID expensive = listing(seller, "Book expensive", 500);
        UUID cheap = listing(seller, "book cheap", 100);
        UUID sold = listing(seller, "Book sold", 1);
        UUID closed = listing(seller, "Book closed", 10);
        listingService.markAsSold(seller.getId(), sold);
        listingService.markAsClosed(seller.getId(), closed);
        assertThat(listingService.search(null, null, ListingSortBy.PRICE, Sort.Direction.ASC))
                .extracting(l -> l.id())
                .containsExactly(cheap, expensive, sold);
        assertThat(listingService.search(
                           "BOOK", ListingCategory.BOOKS, ListingSortBy.PRICE, Sort.Direction.DESC))
                .extracting(l -> l.id())
                .containsExactly(expensive, cheap, sold);
        UUID literal = listing(seller, "100%_! real", 50);
        assertThat(listingService.search("100%_!", null, ListingSortBy.CREATED_AT, Sort.Direction.DESC))
                .extracting(l -> l.id())
                .containsExactly(literal);
        assertThat(listingService.search(
                           null, ListingCategory.TRANSPORT, ListingSortBy.PRICE, Sort.Direction.ASC))
                .isEmpty();
    }

    @Test
    void orderLifecycleLoadsSellerAndBuyerAndPersists() {
        User seller = user("seller"), buyer = user("buyer");
        UUID listingId = listing(seller, "Book", 100);
        var created = orderService.createOrder(buyer.getId(), listingId);
        assertThat(orderService.findOrders(seller.getId(), OrderDirection.INCOMING))
                .singleElement()
                .satisfies(o -> assertThat(o.sellerId()).isEqualTo(seller.getId()));
        assertThat(orderService.findOrders(buyer.getId(), OrderDirection.OUTGOING))
                .singleElement()
                .satisfies(o -> assertThat(o.buyerId()).isEqualTo(buyer.getId()));
        orderService.confirmOrder(seller.getId(), created.id());
        assertThat(orders.findById(created.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        orderService.completeOrder(buyer.getId(), created.id());
        assertThat(orders.findById(created.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThatThrownBy(() -> orderService.cancelOrder(buyer.getId(), created.id()))
                .isInstanceOf(OrderStateConflictException.class);
    }

    @Test
    void invalidOrdersAndCancellation() throws Exception {
        User seller = user("seller"), buyer = user("buyer"), other = user("other");
        UUID id = listing(seller, "Book", 100);
        assertThatThrownBy(() -> orderService.createOrder(seller.getId(), id))
                .isInstanceOf(OrderCreateConflictException.class);
        var order = orderService.createOrder(buyer.getId(), id);
        mvc.perform(patch("/api/orders/" + order.id() + "/confirm")
                            .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/" + order.id() + "/complete")
                            .header("Authorization", "Bearer " + token(buyer)))
                .andExpect(status().isConflict());
        orderService.cancelOrder(seller.getId(), order.id());
        assertThat(orders.findById(order.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        listingService.markAsClosed(seller.getId(), id);
        assertThatThrownBy(() -> orderService.createOrder(buyer.getId(), id))
                .isInstanceOf(OrderCreateConflictException.class);
    }

    @Test
    void transactionRollsBackAllRepositories() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            User seller = user("seller"), buyer = user("buyer");
            orderService.createOrder(buyer.getId(), listing(seller, "Book", 100));
            assertThat(statistics.getStatistics().getOrdersCount()).isEqualTo(1);
            tx.setRollbackOnly();
        });
        var result = statistics.getStatistics();
        assertThat(result.getUsersCount()).isZero();
        assertThat(result.getListingsCount()).isZero();
        assertThat(result.getOrdersCount()).isZero();
    }

    @Test
    void bootstrapCreatesOnlyAccountsAndCanRunTwice() {
        bootstrap.initialize();
        bootstrap.initialize();
        var stats = statistics.getStatistics();
        assertThat(stats.getUsersCount()).isEqualTo(6);
        assertThat(stats.getListingsCount()).isZero();
        assertThat(stats.getOrdersCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_tags", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_notes", Long.class)).isZero();
        assertThat(accounts.login(bootstrapProperties.adminEmail(), bootstrapProperties.adminPassword())
                           .account()
                           .role())
                .isEqualTo(UserRole.ADMIN);
        assertThat(accounts.login("demo1@example.com", bootstrapProperties.demoPassword()).account().role())
                .isEqualTo(UserRole.USER);
    }

    @Test
    void exportRequiresAdminAndContainsAllTablesWithSafeCsv() throws Exception {
        mvc.perform(get("/api/admin/export")).andExpect(status().isUnauthorized());
        User user = user("ordinary");
        mvc.perform(get("/api/admin/export").header("Authorization", "Bearer " + token(user)))
                .andExpect(status().isForbidden());
        bootstrap.initialize();
        User admin = users.findByEmail(bootstrapProperties.adminEmail()).orElseThrow();
        listing(admin, "=SUM(1,2)", 123);
        listing(admin, "Книга \"цитата\"\nстрока", 124);
        byte[] bytes = mvc.perform(get("/api/admin/export").header("Authorization", "Bearer " + token(admin)))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType("application/zip"))
                               .andExpect(header().string(
                                       "Content-Disposition", "attachment; filename=\"domain-export.zip\""))
                               .andReturn()
                               .getResponse()
                               .getContentAsByteArray();
        Map<String, String> contents = new HashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
                contents.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThat(contents.keySet())
                .containsExactlyInAnyOrder("users.csv", "listings.csv", "orders.csv", "categories.csv",
                        "listing_notes.csv", "listing_tags.csv");
        assertThat(contents.get("users.csv")).doesNotContain("password_hash", "$2a$", "$2b$");
        assertThat(contents.get("listings.csv")).contains("\"'=SUM(1,2)\"", "Книга \"\"цитата\"\"\nстрока");
        assertThat(contents.get("orders.csv")).contains("buyer_id");
    }

    @Test
    void tokenRemainsValidUntilExpiryAfterAccountBlocked() throws Exception {
        User user = user("ordinary");
        String token = token(user);
        user.setBlocked(true);
        users.save(user);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThatThrownBy(() -> accounts.login(user.getEmail(), "Password-123"))
                .isInstanceOf(AccountBlockedException.class);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshCannotBeUsedEvenWithoutRevocation() {
        var account = accounts.createAccount("Alice", "alice@example.com", "Password-123");
        jdbc.update(
                "UPDATE refresh_sessions SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE user_id=?",
                account.account().id());
        assertThatThrownBy(() -> accounts.refresh(account.refreshToken()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void staleUpdatesCannotOverwriteNewerState() {
        User seller = user("seller"), buyer = user("buyer");
        UUID id = listing(seller, "Book", 100);
        Listing stale = listings.findById(id).orElseThrow();
        listingService.markAsSold(seller.getId(), id);
        stale.setStatus(ListingStatus.CLOSED);
        assertThatThrownBy(() -> listings.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
        listingService.markAsActive(seller.getId(), id);
        var created = orderService.createOrder(buyer.getId(), id);
        Order staleOrder = orders.findById(created.id()).orElseThrow();
        orderService.confirmOrder(seller.getId(), created.id());
        staleOrder.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> orders.save(staleOrder))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void validationMissingAndUnknownResources() throws Exception {
        User user = user("ordinary");
        mvc.perform(post("/api/listings")
                            .header("Authorization", "Bearer " + token(user))
                            .contentType("application/json")
                            .content("{\"title\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token(user)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound());
        assertThat(users.findById(UUID.randomUUID())).isEmpty();
        assertThat(listings.findById(UUID.randomUUID())).isEmpty();
        assertThat(orders.findById(UUID.randomUUID())).isEmpty();
    }
}
