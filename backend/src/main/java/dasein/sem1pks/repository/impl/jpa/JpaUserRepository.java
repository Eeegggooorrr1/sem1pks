package dasein.sem1pks.repository.impl.jpa;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {

    @Override
    default User save(User user) {
        return saveAndFlush(user);
    }

    @Override
    Optional<User> findById(UUID id);

    @Override
    Optional<User> findByEmail(String email);

    @Override
    boolean existsByEmail(String email);

    @Override
    @Query(value = "SELECT id FROM users WHERE email = :email FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockByEmail(@Param("email") String email);

    @Override
    @Query(value = "SELECT id FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockById(@Param("id") UUID id);
}
