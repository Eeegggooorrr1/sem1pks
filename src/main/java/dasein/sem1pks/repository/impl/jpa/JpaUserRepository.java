package dasein.sem1pks.repository.impl.jpa;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.repository.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("jpa")
public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {

    @Override
    Optional<User> findById(Long id);

    @Override
    Optional<User> findByEmail(String email);

    @Override
    boolean existsByEmail(String email);
}
