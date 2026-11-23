package dasein.sem1pks.exception.forbidden;

public class AdminSelfAccessChangeException extends ForbiddenException {

    public AdminSelfAccessChangeException() {
        super("ADMIN_SELF_ACCESS_CHANGE", "Свою роль и состояние аккаунта изменить нельзя.");
    }
}
