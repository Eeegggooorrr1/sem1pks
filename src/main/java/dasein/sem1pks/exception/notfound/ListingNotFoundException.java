package dasein.sem1pks.exception.notfound;

public class ListingNotFoundException extends NotFoundException {

    public ListingNotFoundException(Long listingId) {
        super("LISTING_NOT_FOUND", "Объявление с id " + listingId + " не найдено");
    }
}
