package dasein.sem1pks.console.api;

public final class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int status() {
        return status;
    }
}
