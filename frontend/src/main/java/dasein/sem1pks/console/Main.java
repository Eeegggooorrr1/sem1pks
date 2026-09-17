package dasein.sem1pks.console;

import dasein.sem1pks.console.api.ApiClient;
import dasein.sem1pks.console.session.TokenStore;
import dasein.sem1pks.console.ui.ConsoleUi;
import dasein.sem1pks.console.config.ClientConfig;

public final class Main {
    public static void main(String[] args) {
        TokenStore tokens = new TokenStore();
        try {
            ClientConfig config = ClientConfig.fromEnvironment();
            new ConsoleUi(new ApiClient(config.apiUrl(), tokens), config.exportDirectory()).run();
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка настройки: " + e.getMessage());
        } finally {
            tokens.clear();
        }
    }
}
