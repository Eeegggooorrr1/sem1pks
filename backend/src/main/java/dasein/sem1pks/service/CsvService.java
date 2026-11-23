package dasein.sem1pks.service;

import dasein.sem1pks.domain.ListingStatus;
import dasein.sem1pks.dto.request.csv.ImportMode;
import dasein.sem1pks.dto.request.listing.ListingFilter;
import dasein.sem1pks.dto.request.listing.ListingInput;
import dasein.sem1pks.dto.request.listing.UpdateListing;
import dasein.sem1pks.dto.response.category.CategoryView;
import dasein.sem1pks.dto.response.csv.ImportError;
import dasein.sem1pks.dto.response.csv.ImportPreview;
import dasein.sem1pks.dto.response.csv.ImportResult;
import dasein.sem1pks.dto.response.listing.ListingView;
import dasein.sem1pks.exception.DomainException;
import dasein.sem1pks.exception.badrequest.InvalidCatalogDataException;
import dasein.sem1pks.exception.conflict.CatalogConflictException;
import dasein.sem1pks.repository.CatalogRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CsvService {
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "code", "title", "description", "price", "status", "categoryCode", "featured", "tags"
    );
    private static final Charset WINDOWS_1251 = Charset.forName("windows-1251");
    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_RECORDS = 1_000;

    private final CatalogRepository repository;
    private final CatalogService listings;
    private final CategoryService categories;
    private final ImportConfirmation confirmations;
    private final Validator validator;
    private final AuditService audit;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] export(ListingFilter filter) {
        var text = new StringWriter();
        text.write('\uFEFF');
        var format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(
                        "code", "title", "description", "price", "status", "categoryCode", "featured", "tags",
                        "id", "archived", "version", "createdAt", "updatedAt", "priority"
                )
                .get();

        try (var printer = new CSVPrinter(text, format)) {
            Map<UUID, String> categoryCodes = new HashMap<>();
            categories.list().forEach(category -> categoryCodes.put(category.id(), category.code()));
            for (var listing : repository.all(filter)) {
                printListing(printer, listing, categoryCodes.get(listing.categoryId()));
            }

            printer.flush();
            return text.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void printListing(CSVPrinter printer, ListingView listing, String categoryCode) throws IOException {
        printer.printRecord(
                safe(listing.code()), safe(listing.title()), safe(listing.description()),
                listing.price().toPlainString().replace('.', ','), listing.status(), categoryCode,
                listing.featured(), safe(String.join(";", listing.tags())), listing.id(), listing.archived(),
                listing.version(), listing.createdAt(), listing.updatedAt(), listing.priority()
        );
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ImportPreview check(UUID actor, byte[] bytes, ImportMode mode) {
        Parsed parsed = parse(decode(bytes), mode);
        if (!parsed.errors().isEmpty()) {
            return new ImportPreview(null, parsed.rows().size(), parsed.errors().size(), parsed.errors());
        }
        if (parsed.rows().isEmpty()) {
            throw new InvalidCatalogDataException("В CSV нет записей");
        }

        String confirmation = confirmations.issue(actor, fingerprint(bytes, mode, parsed.rows()));
        return new ImportPreview(confirmation, parsed.rows().size(), 0, List.of());
    }

    @Transactional
    public ImportResult apply(UUID actor, byte[] bytes, ImportMode mode, String confirmation) {
        Parsed parsed = parse(decode(bytes), mode);
        if (!parsed.errors().isEmpty()) {
            throw new CatalogConflictException("Данные изменились. Проверьте CSV заново");
        }

        UUID importId = confirmations.verify(actor, confirmation, fingerprint(bytes, mode, parsed.rows()));
        int created = (int) parsed.rows().stream().filter(row -> row.existing() == null).count();
        int updated = parsed.rows().size() - created;
        try {
            String description = "Создано: " + created + ", обновлено: " + updated;
            audit.record(actor, "CSV_IMPORT", "IMPORT", importId, description);
        } catch (DuplicateKeyException ex) {
            throw new CatalogConflictException("Импорт уже применен");
        }

        for (Row row : parsed.rows()) {
            if (row.existing() == null) {
                listings.create(actor, row.data());
            } else {
                var update = new UpdateListing(row.existing().version(), row.data());
                listings.update(actor, row.existing().id(), update);
            }
        }

        return new ImportResult(created, updated);
    }

    private String decode(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new InvalidCatalogDataException("Размер CSV должен быть от 1 байта до 1 МБ");
        }
        if (bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 3 && bytes[3] == 4) {
            throw new InvalidCatalogDataException("Выберите CSV, а не файл Excel или ZIP-архив");
        }

        boolean utf16 = bytes.length >= 2 && ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
                || (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF));
        boolean utf8Bom = bytes.length >= 3 && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF;

        String content;
        try {
            content = decode(bytes, utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8);
        } catch (CharacterCodingException ex) {
            if (utf16 || utf8Bom) {
                throw new InvalidCatalogDataException("Файл поврежден. Сохраните его заново как CSV UTF-8");
            }
            try {
                content = decode(bytes, WINDOWS_1251);
            } catch (CharacterCodingException fallbackFailure) {
                throw new InvalidCatalogDataException("Не удалось прочитать CSV. Сохраните его как CSV UTF-8");
            }
        }

        if (content.indexOf('\0') >= 0) {
            throw new InvalidCatalogDataException("Не удалось прочитать CSV. Сохраните его как CSV UTF-8");
        }
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private String decode(byte[] bytes, Charset encoding) throws CharacterCodingException {
        return encoding.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private String fingerprint(byte[] bytes, ImportMode mode, List<Row> rows) {
        var state = new StringBuilder(mode.name()).append(':').append(ImportConfirmation.hash(bytes));
        for (Row row : rows) {
            state.append(':').append(row.data().categoryId()).append(':');
            if (row.existing() != null) {
                state.append(row.existing().id()).append('/').append(row.existing().version());
            }
        }
        return ImportConfirmation.hash(state.toString().getBytes(StandardCharsets.UTF_8));
    }

    private Parsed parse(String content, ImportMode mode) {
        Map<String, CategoryView> categoryCodes = new HashMap<>();
        categories.list().forEach(category -> categoryCodes.put(category.code(), category));

        String firstLine = content.lines().findFirst().orElse("");
        boolean hasExcelDirective = firstLine.equalsIgnoreCase("sep=;") || firstLine.equalsIgnoreCase("sep=,");
        char separator = hasExcelDirective ? firstLine.charAt(4) : separator(firstLine);
        String records = hasExcelDirective ? content.substring(firstLine.length()).stripLeading() : content;

        var format = CSVFormat.DEFAULT.builder()
                .setDelimiter(separator)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .get();
        try (var parser = CSVParser.parse(records, format)) {
            return parseRows(parser, mode, categoryCodes);
        } catch (IOException | UncheckedIOException ex) {
            throw new InvalidCatalogDataException(
                    "Не удалось прочитать CSV. Проверьте разделители и кавычки в файле.");
        } catch (IllegalArgumentException ex) {
            throw new InvalidCatalogDataException(
                    "Не удалось прочитать заголовки CSV. Скачайте пример и сверьте названия столбцов.");
        }
    }

    private char separator(String header) {
        if (header.indexOf('\t') >= 0) {
            return '\t';
        }
        return header.indexOf(';') >= 0 ? ';' : ',';
    }

    private Parsed parseRows(CSVParser parser, ImportMode mode, Map<String, CategoryView> categoryCodes) {
        if (!parser.getHeaderNames().containsAll(REQUIRED_COLUMNS)) {
            throw new InvalidCatalogDataException(
                    "В файле не хватает обязательных столбцов. Скачайте пример и сверьте заголовки.");
        }

        List<Row> rows = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CSVRecord record : parser) {
            if (record.getRecordNumber() > MAX_RECORDS) {
                throw new InvalidCatalogDataException("Не больше 1000 записей в одном файле");
            }
            long rowNumber = record.getRecordNumber() + 1;

            try {
                rows.add(parseRow(record, mode, categoryCodes, seen));
            } catch (DomainException ex) {
                errors.add(new ImportError(rowNumber, ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                errors.add(new ImportError(rowNumber, "Некорректное значение поля"));
            }
        }

        return new Parsed(rows, errors);
    }

    private Row parseRow(
            CSVRecord record,
            ImportMode mode,
            Map<String, CategoryView> categoryCodes,
            Set<String> seen
    ) {
        if (!record.isConsistent()) {
            throw new InvalidCatalogDataException(
                    "Количество значений не совпадает с количеством столбцов. Проверьте разделители.");
        }

        String code = record.get("code");
        if (!seen.add(code)) {
            throw new InvalidCatalogDataException("Код повторяется в файле");
        }
        CategoryView category = categoryCodes.get(record.get("categoryCode"));
        if (category == null) {
            throw new InvalidCatalogDataException("Категория не найдена");
        }

        var data = new ListingInput(
                code,
                unsafed(record.get("title")),
                unsafed(record.get("description")),
                csvPrice(record.get("price")),
                csvStatus(record.get("status")),
                category.id(),
                csvFeatured(record.get("featured")),
                csvTags(record.get("tags"))
        );
        validateRow(data);

        var existing = repository.findCode(code).orElse(null);
        if (existing != null && mode == ImportMode.NEW_ONLY) {
            throw new InvalidCatalogDataException("Код уже существует");
        }
        if (!category.enabled() && (existing == null || !existing.categoryId().equals(category.id()))) {
            throw new InvalidCatalogDataException("Категория отключена");
        }
        return new Row(data, existing);
    }

    private void validateRow(ListingInput data) {
        var violations = validator.validate(data);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> csvField(violation.getPropertyPath().toString()) + ": " + violation.getMessage())
                    .sorted()
                    .findFirst()
                    .orElseThrow();
            throw new InvalidCatalogDataException(message);
        }
    }

    private boolean csvFeatured(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "истина" -> true;
            case "false", "ложь" -> false;
            default -> throw new InvalidCatalogDataException("Выделение: укажите true (да) или false (нет).");
        };
    }

    private Set<String> csvTags(String value) {
        String tags = unsafed(value);
        return tags.isBlank() ? Set.of() : new LinkedHashSet<>(Arrays.asList(tags.split(";", -1)));
    }

    private static BigDecimal csvPrice(String value) {
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new InvalidCatalogDataException("Цена: укажите число, например 1500,50.");
        }
    }

    private static ListingStatus csvStatus(String value) {
        try {
            return ListingStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidCatalogDataException("Статус: укажите ACTIVE, SOLD или CLOSED.");
        }
    }

    private static String csvField(String path) {
        if (path.startsWith("tags")) {
            return "Теги";
        }
        return switch (path) {
            case "code" -> "Код";
            case "title" -> "Название";
            case "description" -> "Описание";
            case "price" -> "Цена";
            default -> "Данные";
        };
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("'")) {
            return "'" + value;
        }
        String stripped = value.stripLeading();
        return !stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0 ? "'" + value : value;
    }

    private String unsafed(String value) {
        if (value.startsWith("''")) {
            return value.substring(1);
        }
        if (value.startsWith("'")) {
            String unquoted = value.substring(1);
            if (!safe(unquoted).equals(unquoted)) {
                return unquoted;
            }
        }
        return value;
    }

    private record Row(
            ListingInput data,
            ListingView existing
    ) {}

    private record Parsed(
            List<Row> rows,
            List<ImportError> errors
    ) {}
}
