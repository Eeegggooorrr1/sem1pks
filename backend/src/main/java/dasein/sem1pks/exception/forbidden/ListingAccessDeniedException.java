package dasein.sem1pks.exception.forbidden;

import java.util.UUID;

public class ListingAccessDeniedException extends ForbiddenException {

    public ListingAccessDeniedException(UUID listingId) {
        super("LISTING_ACCESS_DENIED", "Нет прав для изменения объявления с id " + listingId);
    }
}
