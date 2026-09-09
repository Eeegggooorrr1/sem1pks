package dasein.sem1pks.exception.forbidden;

public class ListingAccessDeniedException extends ForbiddenException {

    public ListingAccessDeniedException(Long listingId) {
        super("LISTING_ACCESS_DENIED", "Нет прав для изменения объявления с id " + listingId);
    }
}
