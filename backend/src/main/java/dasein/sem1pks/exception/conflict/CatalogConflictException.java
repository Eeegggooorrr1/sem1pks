package dasein.sem1pks.exception.conflict;

public class CatalogConflictException extends ConflictException {

    public CatalogConflictException(String message) {
        super("CONFLICT", message);
    }
}
