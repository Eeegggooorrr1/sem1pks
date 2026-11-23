package dasein.sem1pks.exception.notfound;

import java.util.UUID;

public class AccountNotFoundException extends NotFoundException {
    public AccountNotFoundException(String email) {
        super("ACCOUNT_NOT_FOUND", "Аккаунт с почтой " + email + " не найден");
    }

    public AccountNotFoundException(UUID id) {
        super("ACCOUNT_NOT_FOUND", "Аккаунт с id " + id + " не найден");
    }

}
