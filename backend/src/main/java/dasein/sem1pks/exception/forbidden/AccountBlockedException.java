package dasein.sem1pks.exception.forbidden;

public class AccountBlockedException extends ForbiddenException {
    public AccountBlockedException() {
        super("ACCOUNT_BLOCKED", "Аккаунт заблокирован");
    }
}
