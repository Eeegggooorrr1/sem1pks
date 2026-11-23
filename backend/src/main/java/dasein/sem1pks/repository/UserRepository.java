package dasein.sem1pks.repository;

import dasein.sem1pks.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAllByOrderByIdAsc(Pageable pageable);

    Optional<UUID> lockByEmail(String email);

    Optional<UUID> lockById(UUID id);
}
