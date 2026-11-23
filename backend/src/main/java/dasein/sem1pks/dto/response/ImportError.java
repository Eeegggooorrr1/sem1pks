package dasein.sem1pks.dto.response;

public record ImportError(
        long row,
        String message
) {
    public String getUserMessage() { return message; }
}
