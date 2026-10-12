package dasein.sem1pks.javafx.ui;

import dasein.sem1pks.javafx.model.ListingCategory;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

final class UiSupport {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("ELECTRONICS", "Электроника");
        CATEGORY_NAMES.put("REAL_ESTATE", "Недвижимость");
        CATEGORY_NAMES.put("TRANSPORT", "Транспорт");
        CATEGORY_NAMES.put("FURNITURE", "Мебель");
        CATEGORY_NAMES.put("CLOTHING", "Одежда");
        CATEGORY_NAMES.put("SPORTS", "Спорт");
        CATEGORY_NAMES.put("BOOKS", "Книги");
        CATEGORY_NAMES.put("BEAUTY", "Красота");
        CATEGORY_NAMES.put("SERVICES", "Услуги");
        CATEGORY_NAMES.put("JOBS", "Работа");
        CATEGORY_NAMES.put("PETS", "Животные");
        CATEGORY_NAMES.put("GARDEN", "Сад");
        CATEGORY_NAMES.put("CONSTRUCTION", "Строительство");
        CATEGORY_NAMES.put("CHILDREN", "Детское");
        CATEGORY_NAMES.put("OTHER", "Другое");
    }

    private UiSupport() {
    }

    static void error(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    static void info(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    static boolean confirm(String message) {
        return new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL)
                .showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    static String categoryName(String value) {
        return CATEGORY_NAMES.getOrDefault(value, value);
    }

    static Map<String, String> categories() {
        return CATEGORY_NAMES;
    }

    static String statusName(String status) {
        return switch (status) {
            case "ACTIVE" -> "Активно";
            case "SOLD" -> "Нет в наличии";
            case "CLOSED" -> "Закрыто";
            case "PENDING" -> "Ожидает подтверждения";
            case "CONFIRMED" -> "Подтверждено";
            case "COMPLETED" -> "Завершено";
            case "CANCELLED" -> "Отменено";
            default -> status;
        };
    }

    static String price(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString() + " ₽";
    }

    static String date(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.parse(value).format(DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    static String valueForCategoryName(String name) {
        return CATEGORY_NAMES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(ListingCategory.OTHER.name());
    }
}
