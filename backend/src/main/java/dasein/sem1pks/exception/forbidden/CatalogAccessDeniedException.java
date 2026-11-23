package dasein.sem1pks.exception.forbidden;

public class CatalogAccessDeniedException extends ForbiddenException {

    public CatalogAccessDeniedException() {
        super("FORBIDDEN", "Недостаточно прав");
    }
}
