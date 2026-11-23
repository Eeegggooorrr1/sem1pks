package dasein.sem1pks.repository;

public interface BootstrapRepository {

    void lockHistory();

    boolean isApplied(String name);

    void markApplied(String name);
}
