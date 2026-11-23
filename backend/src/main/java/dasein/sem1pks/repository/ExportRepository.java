package dasein.sem1pks.repository;

import java.util.List;
import java.util.function.Consumer;

public interface ExportRepository {

    enum Table {
        USERS("users.csv"),
        LISTINGS("listings.csv"),
        ORDERS("orders.csv"),
        CATEGORIES("categories.csv"),
        NOTES("listing_notes.csv"),
        TAGS("listing_tags.csv");

        private final String fileName;

        Table(String fileName) {
            this.fileName = fileName;
        }

        public String fileName() {
            return fileName;
        }
    }

    void read(Table table, Consumer<List<String>> header, Consumer<List<String>> row);
}
