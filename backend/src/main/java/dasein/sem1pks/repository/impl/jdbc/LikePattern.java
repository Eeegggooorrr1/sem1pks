package dasein.sem1pks.repository.impl.jdbc;

final class LikePattern {

    private LikePattern() {}

    static String contains(String value) {
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
