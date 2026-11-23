package dasein.sem1pks;

import dasein.sem1pks.dto.request.bulk.BulkAction;
import dasein.sem1pks.dto.request.bulk.BulkItem;
import dasein.sem1pks.dto.request.bulk.BulkRequest;
import dasein.sem1pks.dto.request.category.CategoryInput;
import dasein.sem1pks.dto.request.csv.ImportMode;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.request.listing.UpdateListing;
import dasein.sem1pks.dto.request.note.NoteInput;
import dasein.sem1pks.dto.response.analytics.Group;
import dasein.sem1pks.dto.response.listing.ListingView;
import dasein.sem1pks.exception.badrequest.InvalidCatalogDataException;
import dasein.sem1pks.exception.conflict.CatalogConflictException;
import dasein.sem1pks.repository.CatalogRepository;
import dasein.sem1pks.service.AnalyticsService;
import dasein.sem1pks.service.CatalogService;
import dasein.sem1pks.service.CategoryService;
import dasein.sem1pks.service.CsvService;
import dasein.sem1pks.service.NoteService;
import dasein.sem1pks.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class CatalogContract extends ApplicationContract {
    @Autowired CatalogService catalog;
    @Autowired CatalogRepository catalogRepository;
    @Autowired CategoryService categories;
    @Autowired NoteService notes;
    @Autowired CsvService csv;
    @Autowired AnalyticsService analytics;
    @Autowired ObjectMapper json;

    UUID category() {
        return categories.list()
                .stream()
                .filter(c -> c.code().equals("BOOKS"))
                .findFirst()
                .orElseThrow()
                .id();
    }
    ListingInput input(String code, int price, boolean featured, Set<String> tags) {
        return new ListingInput(code, "Книга " + code, "Описание", BigDecimal.valueOf(price),
                ListingStatus.ACTIVE, category(), featured, tags);
    }
    ListingInput changed(ListingView view, String title) {
        return new ListingInput(view.code(), title, view.description(), view.price(), view.status(),
                view.categoryId(), view.featured(), view.tags());
    }
    String body(Object data) {
        return json.writeValueAsString(data);
    }
    String csvRow(String code, int price) {
        return code + ",Книга,Описание," + price + ",ACTIVE,BOOKS,false,test\r\n";
    }
    byte[] csvBytes(String rows) {
        return ("code,title,description,price,status,categoryCode,featured,tags\r\n" + rows)
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void catalogCrudArchiveVersionAndAdminDeletion() throws Exception {
        User owner = user("owner"), other = user("other");
        String response = mvc.perform(post("/api/listings")
                                              .header("Authorization", "Bearer " + token(owner))
                                              .contentType("application/json")
                                              .content(body(input("BOOK-1", 100, false, Set.of("new")))))
                                  .andExpect(status().isCreated())
                                  .andExpect(jsonPath("$.version").value(0))
                                  .andExpect(jsonPath("$.priority").value("LOW"))
                                  .andReturn()
                                  .getResponse()
                                  .getContentAsString();
        UUID id = UUID.fromString(json.readTree(response).get("id").asString());
        var first = catalog.get(id);
        mvc.perform(get("/api/listings/" + id)).andExpect(status().isOk());
        mvc.perform(put("/api/listings/" + id)
                            .header("Authorization", "Bearer " + token(other))
                            .contentType("application/json")
                            .content(body(new UpdateListing(0L, changed(first, "Другой")))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/listings/" + id)
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new UpdateListing(0L, changed(first, "Новое название")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/listings/" + id)
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new UpdateListing(0L, changed(first, "Устарело")))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/listings/" + id + "?version=1")
                            .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/listings/" + id + "/close")
                            .header("Authorization", "Bearer " + token(other))
                            .contentType("application/json")
                            .content("{\"version\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/listings/" + id + "/close")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.archived").value(false));
        mvc.perform(post("/api/orders/listings/" + id).header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/listings/" + id)
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new UpdateListing(2L, changed(first, "Повторное открытие")))))
                .andExpect(status().isConflict());
        other.setRole(UserRole.ADMIN);
        users.save(other);
        mvc.perform(delete("/api/listings/" + id + "?version=2")
                            .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        mvc.perform(get("/api/listings")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/listings?archived=true")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/listings?archived=true").header("Authorization", "Bearer " + token(other)))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(post("/api/listings/" + id + "/restore")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content("{\"version\":3}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/listings/" + id + "/restore")
                            .header("Authorization", "Bearer " + token(other))
                            .contentType("application/json")
                            .content("{\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(4));
        mvc.perform(delete("/api/listings/" + id + "/permanent?version=4")
                            .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/listings/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void allFiltersSortingPaginationAndLiteralSearch() throws Exception {
        User owner = user("owner");
        var low = catalog.create(owner.getId(), input("LOW", 100, false, Set.of("book")));
        var medium = catalog.create(owner.getId(), input("MEDIUM", 10000, false, Set.of("book")));
        var high = catalog.create(owner.getId(), input("HIGH", 50, true, Set.of("special")));
        mvc.perform(get("/api/listings?search=Описание&categoryId=" + category()
                            + "&status=ACTIVE&featured=false&priority=MEDIUM&tag=book&price=9999&priceOp=GT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(medium.id().toString()));
        mvc.perform(get("/api/listings?size=1&page=1&sortBy=PRICE&direction=ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].id").value(low.id().toString()));
        mvc.perform(get("/api/listings?sortBy=TITLE&direction=ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(high.id().toString()));
        mvc.perform(get("/api/listings?sortBy=UPDATED_AT")).andExpect(status().isOk());
        mvc.perform(get("/api/listings").param("search", "%' OR TRUE --"))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/listings?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings?sortBy=desc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings?sortBy=")).andExpect(status().isBadRequest());
    }

    @Test
    void categoryAdministrationAndDisabledCategory() throws Exception {
        User owner = user("owner"), admin = user("admin");
        admin.setRole(UserRole.ADMIN);
        users.save(admin);
        String code = "C" + UUID.randomUUID().toString().substring(0, 8).replace("-", "").toUpperCase();
        var data = new CategoryInput(code, "Категория", true);
        mvc.perform(post("/api/categories")
                            .contentType("application/json")
                            .content(body(data)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/categories")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(data)))
                .andExpect(status().isForbidden());
        String response = mvc.perform(post("/api/categories")
                                              .header("Authorization", "Bearer " + token(admin))
                                              .contentType("application/json")
                                              .content(body(data)))
                                  .andExpect(status().isCreated())
                                  .andReturn()
                                  .getResponse()
                                  .getContentAsString();
        UUID id = UUID.fromString(json.readTree(response).get("id").asString());
        mvc.perform(put("/api/categories/" + id)
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(data)))
                .andExpect(status().isForbidden());
        var listing = catalog.create(owner.getId(),
                new ListingInput("CUSTOM", "Custom", "Text", BigDecimal.TEN, ListingStatus.ACTIVE, id, false,
                        Set.of()));
        assertThat(listings.findById(listing.id())).isPresent();
        mvc.perform(put("/api/categories/" + id)
                            .header("Authorization", "Bearer " + token(admin))
                            .contentType("application/json")
                            .content(body(new CategoryInput(code, "Отключена", false))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/listings")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new ListingInput("CUSTOM2", "Title", "Text", BigDecimal.TEN,
                                    ListingStatus.ACTIVE, id, false, Set.of()))))
                .andExpect(status().isBadRequest());
        assertThat(catalog.update(owner.getId(), listing.id(),
                                  new UpdateListing(listing.version(), changed(listing, "Edited")))
                           .title())
                .isEqualTo("Edited");
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }

    @Test
    void noteLifecycleAuthorPermissionsAndPinnedOrder() throws Exception {
        User owner = user("owner"), other = user("other");
        var listing = catalog.create(owner.getId(), input("NOTES", 100, false, Set.of()));
        var first = notes.create(owner.getId(), listing.id(), new NoteInput("Первая", false));
        var pinned = notes.create(other.getId(), listing.id(), new NoteInput("Закреплена", true));
        mvc.perform(get("/api/listings/" + listing.id() + "/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pinned.id().toString()));
        mvc.perform(put("/api/listings/" + listing.id() + "/notes/" + first.id())
                            .header("Authorization", "Bearer " + token(other))
                            .contentType("application/json")
                            .content(body(new NoteInput("Чужая", false))))
                .andExpect(status().isForbidden());
        assertThat(
                notes.update(owner.getId(), listing.id(), first.id(), new NoteInput("Изменена", true)).body())
                .isEqualTo("Изменена");
        notes.delete(owner.getId(), listing.id(), first.id());
        assertThat(notes.list(listing.id())).hasSize(1);
        mvc.perform(post("/api/listings/" + listing.id() + "/notes")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new NoteInput(" ", false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkReportsEachFailureAndPreservesSuccessfulChanges() throws Exception {
        User owner = user("owner"), other = user("other");
        var first = catalog.create(owner.getId(), input("BULK1", 10, false, Set.of()));
        var second = catalog.create(other.getId(), input("BULK2", 20, false, Set.of()));
        var batch = new BulkRequest(List.of(new BulkItem(first.id(), 0L), new BulkItem(second.id(), 0L),
                                            new BulkItem(UUID.randomUUID(), 0L)),
                BulkAction.ARCHIVE, null, null);
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(batch)))
                .andExpect(status().isForbidden());
        owner.setRole(UserRole.ADMIN);
        users.save(owner);
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(batch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful.length()").value(2))
                .andExpect(jsonPath("$.errors.length()").value(1));
        assertThat(catalog.get(first.id()).archived()).isTrue();
        sendBulk(owner,
                new BulkRequest(List.of(new BulkItem(first.id(), 1L)), BulkAction.RESTORE, null, null));
        sendBulk(owner,
                new BulkRequest(
                        List.of(new BulkItem(first.id(), 2L)), BulkAction.STATUS, ListingStatus.SOLD, null));
        sendBulk(owner,
                new BulkRequest(List.of(new BulkItem(first.id(), 3L)), BulkAction.ADD_TAG, null, "tag"));
        assertThat(catalog.get(first.id()).tags()).contains("tag");
        sendBulk(owner,
                new BulkRequest(List.of(new BulkItem(first.id(), 4L)), BulkAction.REMOVE_TAG, null, "tag"));
        assertThat(catalog.get(first.id()).tags()).isEmpty();
        assertThat(catalog.get(first.id()).status()).isEqualTo(ListingStatus.SOLD);
        assertThat(catalog.get(first.id()).version()).isEqualTo(5);
    }

    private void sendBulk(User admin, BulkRequest request) throws Exception {
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(admin))
                            .contentType("application/json")
                            .content(body(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful.length()").value(1));
    }

    @Test
    void csvPreviewReportsRowsAndOnlyCheckedFilesCanBeApplied() throws Exception {
        User owner = user("owner"), other = user("other");
        var invalid = csv.check(
                owner.getId(), csvBytes(csvRow("VALID", 10) + csvRow("BAD", -1)), ImportMode.NEW_ONLY);
        assertThat(invalid.validRows()).isEqualTo(1);
        assertThat(invalid.invalidRows()).isEqualTo(1);
        assertThat(invalid.confirmation()).isNull();
        assertThat(analytics.get().total()).isZero();
        var bytes = csvBytes(csvRow("NEW", 10));
        var preview = csv.check(owner.getId(), bytes, ImportMode.NEW_ONLY);
        assertThatThrownBy(() -> csv.apply(other.getId(), bytes, ImportMode.NEW_ONLY, preview.confirmation()))
                .isInstanceOf(CatalogConflictException.class);
        assertThat(csv.apply(owner.getId(), bytes, ImportMode.NEW_ONLY, preview.confirmation()).created())
                .isEqualTo(1);
        assertThatThrownBy(() -> csv.apply(owner.getId(), bytes, ImportMode.NEW_ONLY, preview.confirmation()))
                .isInstanceOf(CatalogConflictException.class);
        assertThat(csv.check(owner.getId(), csvBytes(csvRow("NEW", 10)), ImportMode.NEW_ONLY).invalidRows())
                .isEqualTo(1);
        var upsert = csv.check(owner.getId(), csvBytes(csvRow("NEW", 20)), ImportMode.UPSERT);
        assertThat(csv.apply(owner.getId(), csvBytes(csvRow("NEW", 20)), ImportMode.UPSERT,
                              upsert.confirmation())
                           .updated())
                .isEqualTo(1);
        assertThat(catalogRepository.findCode("NEW").orElseThrow().price()).isEqualByComparingTo("20");
        var file = new MockMultipartFile("file", "input.csv", "text/csv", csvBytes(csvRow("HTTP", 30)));
        mvc.perform(multipart("/api/listings/import/check")
                            .file(file)
                            .param("mode", "NEW_ONLY")
                            .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isForbidden());
        owner.setRole(UserRole.ADMIN);
        users.save(owner);
        mvc.perform(multipart("/api/listings/import/check")
                            .file(file)
                            .param("mode", "NEW_ONLY")
                            .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validRows").value(1));
    }

    @Test
    void csvRejectsConcurrentChangesAndRollsBackWholeImport() {
        User owner = user("owner");
        var listing = catalog.create(owner.getId(), input("EXISTING", 100, false, Set.of()));
        var bytes = csvBytes(csvRow("NEW", 5) + csvRow("EXISTING", 50));
        var preview = csv.check(owner.getId(), bytes, ImportMode.UPSERT);
        catalog.update(owner.getId(), listing.id(), new UpdateListing(0L, changed(listing, "Newer")));
        assertThatThrownBy(() -> csv.apply(owner.getId(), bytes, ImportMode.UPSERT, preview.confirmation()))
                .isInstanceOf(CatalogConflictException.class);
        assertThat(catalogRepository.findCode("NEW")).isEmpty();
        assertThat(catalog.get(listing.id()).title()).isEqualTo("Newer");
    }

    @Test
    void csvExportRespectsFiltersAndRoundTripsQuotesNewlinesAndFormulaText() throws Exception {
        User owner = user("owner");
        var data = new ListingInput("CSV", "=SUM(1,2)", "Строка \"один\"\nдва", BigDecimal.TEN,
                ListingStatus.ACTIVE, category(), true, Set.of("-tag"));
        catalog.create(owner.getId(), data);
        catalog.create(owner.getId(), input("OTHER", 50, false, Set.of()));
        var filter = new ListingFilter();
        filter.setFeatured(true);
        byte[] bytes = mvc.perform(get("/api/listings/export?featured=true"))
                               .andExpect(status().isOk())
                               .andExpect(content().contentType("text/csv;charset=UTF-8"))
                               .andReturn()
                               .getResponse()
                               .getContentAsByteArray();
        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .startsWith("\uFEFFcode;title;description;")
                .contains("'=SUM(1,2)", "10,00")
                .doesNotContain("OTHER");
        var preview = csv.check(owner.getId(), bytes, ImportMode.UPSERT);
        assertThat(preview.errors()).isEmpty();
        csv.apply(owner.getId(), bytes, ImportMode.UPSERT, preview.confirmation());
        var listing = catalogRepository.findCode("CSV").orElseThrow();
        assertThat(listing.title()).isEqualTo(data.title());
        assertThat(listing.description()).isEqualTo(data.description());
        assertThat(listing.tags()).containsExactly("-tag");
    }

    @Test
    void analyticsAndAuditReflectChangesAndCorrelation() throws Exception {
        User owner = user("owner");
        var first = catalog.create(owner.getId(), input("ONE", 100, false, Set.of()));
        var second = catalog.create(owner.getId(), input("TWO", 200, true, Set.of()));
        User admin = user("admin");
        admin.setRole(UserRole.ADMIN);
        users.save(admin);
        mvc.perform(delete("/api/listings/" + second.id() + "?version=0")
                            .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());
        var stats = analytics.get();
        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.active()).isEqualTo(1);
        assertThat(stats.archived()).isEqualTo(1);
        assertThat(stats.featured()).isEqualTo(1);
        assertThat(stats.averagePrice()).isEqualByComparingTo("150");
        assertThat(stats.byCategory()).contains(new Group(category().toString(), 2));
        assertThat(stats.byPriority()).contains(new Group("LOW", 1), new Group("HIGH", 1));
        mvc.perform(put("/api/listings/" + first.id())
                            .header("Authorization", "Bearer " + token(owner))
                            .header("X-Correlation-ID", "catalog-test")
                            .contentType("application/json")
                            .content(body(new UpdateListing(0L, changed(first, "Changed")))))
                .andExpect(status().isOk());
        assertThat(
                jdbc.queryForObject(
                        "SELECT count(*) FROM audit_events WHERE correlation_id='catalog-test' AND event_type='LISTING_UPDATE'",
                        Long.class))
                .isEqualTo(1);
        mvc.perform(get("/api/analytics").header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/analytics").header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void uniqueCodesAndDtoValidationReturnClientErrors() throws Exception {
        User owner = user("owner");
        catalog.create(owner.getId(), input("UNIQUE", 100, false, Set.of()));
        mvc.perform(post("/api/listings")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(input("UNIQUE", 10, false, Set.of()))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/listings/" + UUID.randomUUID())
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content("{\"data\":{}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content("{\"items\":[],\"action\":\"ARCHIVE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/listings/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void csvRejectsMalformedFilesAndChangedPreviews() throws Exception {
        User owner = user("owner");
        owner.setRole(UserRole.ADMIN);
        users.save(owner);
        assertThatThrownBy(
                ()
                        -> csv.check(owner.getId(), "wrong,headers\r\n1,2".getBytes(StandardCharsets.UTF_8),
                                ImportMode.NEW_ONLY))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThatThrownBy(
                () -> csv.check(owner.getId(), new byte[] {(byte) 0xC3, (byte) 0x28}, ImportMode.NEW_ONLY))
                .isInstanceOf(InvalidCatalogDataException.class);
        assertThat(csv.check(owner.getId(), csvBytes(csvRow("SAME", 10) + csvRow("SAME", 20)),
                              ImportMode.NEW_ONLY)
                           .invalidRows())
                .isEqualTo(1);
        var preview = csv.check(owner.getId(), csvBytes(csvRow("EXPIRED", 10)), ImportMode.NEW_ONLY);
        var changedFile =
                new MockMultipartFile("file", "changed.csv", "text/csv", csvBytes(csvRow("EXPIRED", 20)));
        mvc.perform(multipart("/api/listings/import/apply")
                            .file(changedFile)
                            .param("mode", "NEW_ONLY")
                            .param("confirmation", preview.confirmation())
                            .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.trace").doesNotExist());
        var file = new MockMultipartFile("file", "wrong.csv", "text/csv", csvBytes(csvRow("BAD", -1)));
        mvc.perform(multipart("/api/listings/import/check")
                            .file(file)
                            .param("mode", "NEW_ONLY")
                            .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidRows").value(1));
    }

    @Test
    void csvImportsWindows1251AndExcelUnicodeTextWithoutLosingCyrillic() {
        User owner = user("owner");
        String header = "code;title;description;price;status;categoryCode;featured;tags\r\n";
        byte[] windows = (header + "WIN;Книжная полка;Самовывоз;15,25;ACTIVE;BOOKS;ИСТИНА;книги\r\n")
                                 .getBytes(java.nio.charset.Charset.forName("windows-1251"));
        byte[] unicode = ((header + "UNI;Книга;Твердый переплет;20,00;ACTIVE;BOOKS;false;книги\r\n")
                                  .replace(';', '\t'))
                                 .getBytes(StandardCharsets.UTF_16);

        for (byte[] file : List.of(windows, unicode)) {
            var preview = csv.check(owner.getId(), file, ImportMode.NEW_ONLY);
            assertThat(preview.errors()).isEmpty();
            assertThat(csv.apply(owner.getId(), file, ImportMode.NEW_ONLY, preview.confirmation()).created())
                    .isEqualTo(1);
        }
        var listing = catalogRepository.findCode("WIN").orElseThrow();
        assertThat(listing.title()).isEqualTo("Книжная полка");
        assertThat(listing.price()).isEqualByComparingTo("15.25");
        assertThat(listing.tags()).containsExactly("книги");
        assertThat(catalogRepository.findCode("UNI").orElseThrow().description())
                .isEqualTo("Твердый переплет");
    }

    @Test
    void allDomainIdColumnsUseUuid() {
        var ids = jdbc.queryForList("""
                SELECT table_name||'.'||column_name FROM information_schema.columns
                WHERE table_schema='public' AND column_name ~ '(^id$|_id$)'
                  AND column_name<>'correlation_id' AND data_type<>'uuid'
                """, String.class);
        assertThat(ids).isEmpty();
    }

    @Test
    void permanentDeletionCascadesNotesTagsAndOrders() throws Exception {
        User owner = user("owner"), buyer = user("buyer");
        var listing = catalog.create(owner.getId(), input("DELETE", 100, false, Set.of("tag")));
        notes.create(owner.getId(), listing.id(), new NoteInput("Note", false));
        orderService.createOrder(buyer.getId(), listing.id());
        owner.setRole(UserRole.ADMIN);
        users.save(owner);
        mvc.perform(delete("/api/listings/" + listing.id() + "/permanent?version=0")
                            .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_notes", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listing_tags", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Long.class)).isZero();
    }

    @Test
    void orderCanBePlacedUsingTheListingUuidAndAppearsInBothDirections() throws Exception {
        User seller = user("seller"), buyer = user("buyer");
        var listing = catalog.create(seller.getId(), input("BUYABLE", 100, false, Set.of()));
        mvc.perform(post("/api/orders/listings/" + listing.id())
                            .header("Authorization", "Bearer " + token(seller)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/orders/listings/" + listing.id())
                            .header("Authorization", "Bearer " + token(buyer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listing.id().toString()));
        mvc.perform(get("/api/orders?direction=OUTGOING").header("Authorization", "Bearer " + token(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listingTitle").value(listing.title()));
        mvc.perform(get("/api/orders?direction=INCOMING").header("Authorization", "Bearer " + token(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].listingId").value(listing.id().toString()));
    }

    @Test
    void bulkHttpValidatesActionsAndReturnsPartialResult() throws Exception {
        User owner = user("owner");
        owner.setRole(UserRole.ADMIN);
        users.save(owner);
        var listing = catalog.create(owner.getId(), input("BULKHTTP", 100, false, Set.of()));
        var items = List.of(new BulkItem(listing.id(), 0L), new BulkItem(UUID.randomUUID(), 0L));
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new BulkRequest(items, BulkAction.ADD_TAG, null, null))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/listings/bulk")
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType("application/json")
                            .content(body(new BulkRequest(items, BulkAction.ARCHIVE, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful.length()").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1));
    }
}
