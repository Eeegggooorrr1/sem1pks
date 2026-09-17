package dasein.sem1pks.exception.conflict;

public class ListingStateConflictException extends ConflictException {
    public ListingStateConflictException() {
        super("LISTING_STATE_CONFLICT", "невозможно изменить статус объявления");
    }
}
