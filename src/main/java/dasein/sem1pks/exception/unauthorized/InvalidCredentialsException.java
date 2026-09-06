package dasein.sem1pks.exception.unauthorized;

public class InvalidCredentialsException extends UnauthorizedException{
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "неверная почта или пароль");
    }
}
