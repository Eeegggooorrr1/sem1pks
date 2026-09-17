package dasein.sem1pks;

import dasein.sem1pks.bootstrap.BootstrapService;
import dasein.sem1pks.domain.*;
import dasein.sem1pks.dto.request.*;
import dasein.sem1pks.repository.*;
import dasein.sem1pks.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.zip.ZipInputStream;
import static org.assertj.core.api.Assertions.*;
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
    @Autowired dasein.sem1pks.bootstrap.BootstrapProperties bootstrapProperties;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtService jwt;

    @BeforeEach
    void reset() {
        assertThat(jdbc.queryForObject("SELECT current_database()",String.class)).endsWith("_test");
        jdbc.execute("TRUNCATE orders,listings,users,bootstrap_history RESTART IDENTITY CASCADE");
    }

    User user(String name) {
        var response = accounts.createAccount(name, name+"@example.com", "Password-123");
        return users.findById(response.account().id()).orElseThrow();
    }
    Long listing(User seller, String title, int price) {
        return listingService.createListing(seller.getId(),title,"Описание",BigDecimal.valueOf(price),ListingCategory.BOOKS).id();
    }
    String token(User user) {
        return jwt.generateToken(user.getId(),user.getEmail(),user.getRole().name(),user.isBlocked());
    }

    @Test void registrationLoginAndDuplicateEmail() throws Exception {
        var registered = accounts.createAccount("Alice"," Alice@Example.com ","Password-123");
        assertThat(accounts.login("ALICE@example.com","Password-123").account().id()).isEqualTo(registered.account().id());
        assertThat(users.findByEmail("alice@example.com").orElseThrow().getPasswordHash()).doesNotContain("Password-123");
        mvc.perform(post("/api/users/register").contentType("application/json")
            .content("{\"username\":\"Alice\",\"email\":\"alice@example.com\",\"password\":\"Password-123\"}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/users/login").contentType("application/json")
            .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
    }

    @Test void accountDtoValidatesNormalizedNameAndUtf8Password() throws Exception {
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {"username":" a ","email":"short@example.com","password":"Password-123"}
            """)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details.username").exists());
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {"username":"Alice","email":"long@example.com","password":"%s"}
            """.formatted("я".repeat(37))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.password").exists());
        mvc.perform(post("/api/users/register").contentType("application/json").content("""
            {"username":" Alice ","email":"valid@example.com","password":"%s"}
            """.formatted("я".repeat(36))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.account.username").value("Alice"));
    }

    @Test void listingStatusesArePersistedAndOwnershipChecked() throws Exception {
        User seller=user("seller"), other=user("other");
        Long id=listing(seller,"Book",100);
        assertThat(listingService.findMyListings(seller.getId())).singleElement()
            .satisfies(l->assertThat(l.ownerId()).isEqualTo(seller.getId()));
        mvc.perform(patch("/api/listings/"+id+"/sold").header("Authorization","Bearer "+token(other)))
            .andExpect(status().isForbidden());
        listingService.markAsSold(seller.getId(),id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.SOLD);
        listingService.markAsActive(seller.getId(),id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.ACTIVE);
        listingService.markAsClosed(seller.getId(),id);
        assertThat(listings.findById(id).orElseThrow().getStatus()).isEqualTo(ListingStatus.CLOSED);
    }

    @Test void searchFiltersSortsAndEscapesLiteralPrefix() {
        User seller=user("seller");
        Long expensive=listing(seller,"Book expensive",500);
        Long cheap=listing(seller,"book cheap",100);
        Long sold=listing(seller,"Book sold",1);
        Long closed=listing(seller,"Book closed",10);
        listingService.markAsSold(seller.getId(),sold);
        listingService.markAsClosed(seller.getId(),closed);
        assertThat(listingService.search(null,null,ListingSortBy.PRICE,Sort.Direction.ASC))
            .extracting(l->l.id()).containsExactly(cheap,expensive,sold);
        assertThat(listingService.search("BOOK",ListingCategory.BOOKS,ListingSortBy.PRICE,Sort.Direction.DESC))
            .extracting(l->l.id()).containsExactly(expensive,cheap,sold);
        Long literal=listing(seller,"100%_! real",50);
        assertThat(listingService.search("100%_!",null,ListingSortBy.CREATED_AT,Sort.Direction.DESC))
            .extracting(l->l.id()).containsExactly(literal);
        assertThat(listingService.search(null,ListingCategory.TRANSPORT,ListingSortBy.PRICE,Sort.Direction.ASC)).isEmpty();
    }

    @Test void orderLifecycleLoadsSellerAndBuyerAndPersists() {
        User seller=user("seller"), buyer=user("buyer");
        Long listingId=listing(seller,"Book",100);
        var created=orderService.createOrder(buyer.getId(),listingId);
        assertThat(orderService.findOrders(seller.getId(),OrderDirection.INCOMING)).singleElement()
            .satisfies(o->assertThat(o.sellerId()).isEqualTo(seller.getId()));
        assertThat(orderService.findOrders(buyer.getId(),OrderDirection.OUTGOING)).singleElement()
            .satisfies(o->assertThat(o.buyerId()).isEqualTo(buyer.getId()));
        orderService.confirmOrder(seller.getId(),created.id());
        assertThat(orders.findById(created.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        orderService.completeOrder(buyer.getId(),created.id());
        assertThat(orders.findById(created.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThatThrownBy(()->orderService.cancelOrder(buyer.getId(),created.id()))
            .isInstanceOf(dasein.sem1pks.exception.conflict.OrderStateConflictException.class);
    }

    @Test void invalidOrdersAndCancellation() throws Exception {
        User seller=user("seller"),buyer=user("buyer"),other=user("other");
        Long id=listing(seller,"Book",100);
        assertThatThrownBy(()->orderService.createOrder(seller.getId(),id))
            .isInstanceOf(dasein.sem1pks.exception.conflict.OrderCreateConflictException.class);
        var order=orderService.createOrder(buyer.getId(),id);
        mvc.perform(patch("/api/orders/"+order.id()+"/confirm").header("Authorization","Bearer "+token(other)))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/"+order.id()+"/complete").header("Authorization","Bearer "+token(buyer)))
            .andExpect(status().isConflict());
        orderService.cancelOrder(seller.getId(),order.id());
        assertThat(orders.findById(order.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        listingService.markAsClosed(seller.getId(),id);
        assertThatThrownBy(()->orderService.createOrder(buyer.getId(),id))
            .isInstanceOf(dasein.sem1pks.exception.conflict.OrderCreateConflictException.class);
    }

    @Test void transactionRollsBackAllRepositories() {
        new TransactionTemplate(transactions).executeWithoutResult(tx->{
            User seller=user("seller"),buyer=user("buyer");
            orderService.createOrder(buyer.getId(),listing(seller,"Book",100));
            assertThat(statistics.getStatistics().getOrdersCount()).isEqualTo(1);
            tx.setRollbackOnly();
        });
        var result=statistics.getStatistics();
        assertThat(result.getUsersCount()).isZero();
        assertThat(result.getListingsCount()).isZero();
        assertThat(result.getOrdersCount()).isZero();
    }

    @Test void bootstrapIsIdempotentAndHashesPasswords() {
        bootstrap.initialize();
        bootstrap.initialize();
        var stats=statistics.getStatistics();
        assertThat(stats.getUsersCount()).isEqualTo(6);
        assertThat(stats.getListingsCount()).isEqualTo(10);
        assertThat(stats.getOrdersCount()).isEqualTo(5);
        assertThat(accounts.login(bootstrapProperties.adminEmail(),bootstrapProperties.adminPassword()).account().role()).isEqualTo(UserRole.ADMIN);
        assertThat(accounts.login("demo1@example.com",bootstrapProperties.demoPassword()).account().role()).isEqualTo(UserRole.USER);
    }

    @Test void exportRequiresAdminAndContainsAllTablesWithSafeCsv() throws Exception {
        mvc.perform(get("/api/admin/export")).andExpect(status().isUnauthorized());
        User user=user("ordinary");
        mvc.perform(get("/api/admin/export").header("Authorization","Bearer "+token(user))).andExpect(status().isForbidden());
        bootstrap.initialize();
        User admin=users.findByEmail(bootstrapProperties.adminEmail()).orElseThrow();
        listing(admin,"=SUM(1,2)",123);
        listing(admin,"Книга \"цитата\"\nстрока",124);
        byte[] bytes=mvc.perform(get("/api/admin/export").header("Authorization","Bearer "+token(admin)))
            .andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
            .andExpect(header().string("Content-Disposition","attachment; filename=\"domain-export.zip\""))
            .andReturn().getResponse().getContentAsByteArray();
        Map<String,String> contents=new HashMap<>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) contents.put(entry.getName(),new String(zip.readAllBytes(),StandardCharsets.UTF_8));
        }
        assertThat(contents.keySet()).containsExactlyInAnyOrder("users.csv","listings.csv","orders.csv");
        assertThat(contents.get("users.csv")).doesNotContain("password_hash","$2a$","$2b$");
        assertThat(contents.get("listings.csv")).contains("\"'=SUM(1,2)\"","Книга \"\"цитата\"\"\nстрока");
        assertThat(contents.get("orders.csv")).contains("buyer_id");
    }

    @Test void tokenRemainsValidUntilExpiryAfterAccountBlocked() throws Exception {
        User user=user("ordinary");
        String token=token(user);
        user.setBlocked(true); users.save(user);
        mvc.perform(get("/api/listings/my").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThatThrownBy(()->accounts.login(user.getEmail(),"Password-123"))
            .isInstanceOf(dasein.sem1pks.exception.unauthorized.InvalidCredentialsException.class);
        mvc.perform(get("/api/listings/my").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }

    @Test void staleUpdatesCannotOverwriteNewerState() {
        User seller=user("seller"), buyer=user("buyer");
        Long id=listing(seller,"Book",100);
        Listing stale=listings.findById(id).orElseThrow();
        listingService.markAsSold(seller.getId(),id);
        stale.setStatus(ListingStatus.CLOSED);
        assertThatThrownBy(()->listings.save(stale))
            .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        listingService.markAsActive(seller.getId(),id);
        var created=orderService.createOrder(buyer.getId(),id);
        Order staleOrder=orders.findById(created.id()).orElseThrow();
        orderService.confirmOrder(seller.getId(),created.id());
        staleOrder.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(()->orders.save(staleOrder))
            .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test void validationMissingAndUnknownResources() throws Exception {
        User user=user("ordinary");
        mvc.perform(post("/api/listings").header("Authorization","Bearer "+token(user)).contentType("application/json")
            .content("{\"title\":\"\",\"price\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").header("Authorization","Bearer "+token(user))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/listings/99999/sold").header("Authorization","Bearer "+token(user))).andExpect(status().isNotFound());
        assertThat(users.findById(99999L)).isEmpty();
        assertThat(listings.findById(99999L)).isEmpty();
        assertThat(orders.findById(99999L)).isEmpty();
    }
}
