package dasein.sem1pks.exception.notfound;

public class CatalogEntryNotFoundException extends NotFoundException {

    public CatalogEntryNotFoundException() {
        super("NOT_FOUND", "Запись не найдена");
    }
}
