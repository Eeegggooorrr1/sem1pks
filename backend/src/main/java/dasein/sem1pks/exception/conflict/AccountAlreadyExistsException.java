package dasein.sem1pks.exception.conflict;

public class AccountAlreadyExistsException extends ConflictException {
    public AccountAlreadyExistsException(String email) {
        super("ACCOUNT_ALREADY_EXISTS", "Аккаунт с почтой " + email + " уже существует");
    }
}