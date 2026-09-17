package dasein.sem1pks.console.ui;

import dasein.sem1pks.console.api.ApiClient;
import dasein.sem1pks.console.api.ApiException;
import dasein.sem1pks.console.dto.account.*;
import dasein.sem1pks.console.dto.listing.*;
import dasein.sem1pks.console.dto.order.*;
import dasein.sem1pks.console.dto.statistics.*;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ConsoleUi {
    private static final String MENU = """
            
            1 Войти             2 Зарегистрироваться   3 Поиск объявлений
            4 Мои объявления    5 Создать объявление   6 Статус объявления
            7 Заказать          8 Мои заказы          9 Статус заказа
            10 Статистика       11 Экспорт (админ)     12 Закрыть объявление (админ)
            13 Выйти из аккаунта                       0 Завершить
            """;
    private static final String CATEGORIES = """
            ELECTRONICS, REAL_ESTATE, TRANSPORT, FURNITURE, CLOTHING, SPORTS, BOOKS,
            BEAUTY, SERVICES, JOBS, PETS, GARDEN, CONSTRUCTION, CHILDREN, OTHER
            """;

    private final ApiClient api;
    private final Path exportDirectory;
    private final BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    public ConsoleUi(ApiClient api, Path exportDirectory) {
        this.api = api;
        this.exportDirectory = exportDirectory;
    }

    public void run() {
        System.out.println("Сервис объявлений — " + api.baseUrl());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.print(MENU);
                    String choice = read("> ");
                    if ("0".equals(choice)) {
                        return;
                    }
                    execute(choice);
                } catch (EOFException e) {
                    System.out.println("\nДо свидания");
                    return;
                } catch (ApiException | IllegalArgumentException | IOException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                }
            }
        } finally {
            api.accounts().logout();
        }
    }

    private void execute(String choice) throws IOException {
        switch (choice) {
            case "1" -> show(api.accounts().login(new LoginRequest(read("Email: "), password())));
            case "2" -> show(api.accounts().register(new RegisterRequest(read("Имя: "), read("Email: "), password())));
            case "3" -> {
                String prefix = read("Начало названия (Enter — любое): ");
                System.out.print(CATEGORIES);
                String category = upper(read("Категория (Enter — любая): "));
                String sort = withDefault(upper(read("PRICE / CREATED_AT [CREATED_AT]: ")), "CREATED_AT");
                String direction = withDefault(upper(read("ASC / DESC [DESC]: ")), "DESC");
                show(api.listings().search(prefix, category, sort, direction));
            }
            case "4" -> show(api.listings().mine());
            case "5" -> {
                String title = read("Название: ");
                String description = read("Описание: ");
                BigDecimal price = new BigDecimal(read("Цена: ").replace(',', '.'));
                if (price.signum() <= 0 || price.compareTo(new BigDecimal("99999999.99")) > 0
                        || price.stripTrailingZeros().scale() > 2) {
                    throw new IllegalArgumentException("Цена: от 0.01 до 99999999.99, максимум 2 знака после запятой");
                }
                System.out.print(CATEGORIES);
                String category = withDefault(upper(read("Категория [OTHER]: ")), "OTHER");
                show(api.listings().create(new ListingCreateRequest(title, description, price, category)));
            }
            case "6" -> {
                long id = id("ID объявления: ");
                show(api.listings().changeStatus(id, read("sold / active / closed: ")));
            }
            case "7" -> show(api.orders().create(id("ID объявления: ")));
            case "8" -> show(api.orders().find(withDefault(upper(read("INCOMING / OUTGOING [OUTGOING]: ")), "OUTGOING")));
            case "9" -> {
                long id = id("ID заказа: ");
                show(api.orders().changeStatus(id, read("confirm / complete / cancel: ")));
            }
            case "10" -> show(api.statistics().get());
            case "11" -> {
                String fallback = exportDirectory.resolve("domain-export.zip").toString();
                Path path = Path.of(withDefault(read("Сохранить в [" + fallback + "]: "), fallback));
                System.out.println("Сохранено: " + api.admin().export(path).toAbsolutePath());
            }
            case "12" -> show(api.listings().changeStatus(id("ID объявления: "), "closed/admin"));
            case "13" -> {
                api.accounts().logout();
                System.out.println("Вы вышли из аккаунта");
            }
            default -> System.out.println("Выберите номер из меню");
        }
    }

    private String read(String prompt) throws IOException {
        return readRaw(prompt).trim();
    }

    private String readRaw(String prompt) throws IOException {
        String value;
        if (System.console() != null) {
            value = System.console().readLine("%s", prompt);
        } else {
            System.out.print(prompt);
            System.out.flush();
            value = input.readLine();
        }
        if (value == null) {
            throw new EOFException();
        }
        return value;
    }

    private String password() throws IOException {
        if (System.console() == null) {
            System.out.println("Нет интерактивного терминала: пароль может отображаться при вводе.");
            return readRaw("Пароль: ");
        }
        char[] value = System.console().readPassword("Пароль: ");
        if (value == null) {
            throw new EOFException();
        }
        try {
            return new String(value);
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private long id(String prompt) throws IOException {
        long value = Long.parseLong(read(prompt));
        if (value <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
        return value;
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    private static String withDefault(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }

    private void show(Object value) {
        if (value == null) {
            System.out.println("Готово");
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                System.out.println("Список пуст");
            }
            list.forEach(this::show);
        } else if (value instanceof ListingResponse listing) {
            System.out.printf("#%d | %s | %s | %s | %s | владелец #%d%n",
                    listing.id(), listing.title(), listing.price().toPlainString(),
                    listing.status(), listing.category(), listing.ownerId());
            System.out.println("  " + (listing.description() == null ? "" : listing.description()));
        } else if (value instanceof OrderResponse order) {
            System.out.printf("Заказ #%d | объявление #%d | %s | покупатель #%d | продавец #%d%n",
                    order.id(), order.listingId(), order.status(), order.buyerId(), order.sellerId());
        } else if (value instanceof AccountResponse account) {
            System.out.printf("%s (%s), роль %s%n", account.username(), account.email(), account.role());
        } else if (value instanceof StatisticsResponse statistics) {
            System.out.printf("Пользователей: %d; объявлений: %d; заказов: %d%n",
                    statistics.usersCount(), statistics.listingsCount(), statistics.ordersCount());
        }
    }
}
