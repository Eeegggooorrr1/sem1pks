package dasein.sem1pks.exception.badrequest;

public class InvalidCatalogDataException extends BadRequestException {

    public InvalidCatalogDataException(String message) {
        super("INVALID_DATA", message);
    }
}
