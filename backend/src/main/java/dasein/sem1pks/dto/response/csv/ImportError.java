package dasein.sem1pks.dto.response.csv;

public record ImportError(
        long row,
        String message
) {
    public String getUserMessage() { return message; }
}
