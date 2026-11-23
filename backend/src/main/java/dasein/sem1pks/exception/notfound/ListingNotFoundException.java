package dasein.sem1pks.exception.notfound;

import java.util.UUID;

public class ListingNotFoundException extends NotFoundException {

    public ListingNotFoundException(UUID listingId) {
        super("LISTING_NOT_FOUND", "Объявление с id " + listingId + " не найдено");
    }
}
