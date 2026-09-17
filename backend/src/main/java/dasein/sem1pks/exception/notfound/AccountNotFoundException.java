package dasein.sem1pks.exception.notfound;

public class AccountNotFoundException extends NotFoundException {
    public AccountNotFoundException(String email) {
        super("ACCOUNT_NOT_FOUND", "Аккаунт с почтой " + email + " не найден");
    }

    public AccountNotFoundException(Long id) {
        super("ACCOUNT_NOT_FOUND", "Аккаунт с id " + id + " не найден");
    }

}
