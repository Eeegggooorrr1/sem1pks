package dasein.sem1pks.dto.response.account;

public record AuthResponse(
        String accessToken,
        AccountResponse account,
        String refreshToken
) {}
