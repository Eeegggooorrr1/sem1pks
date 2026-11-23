package dasein.sem1pks.service;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.dto.request.admin.AdminUserRequest;
import dasein.sem1pks.dto.response.admin.UserView;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.exception.conflict.AccountAlreadyExistsException;
import dasein.sem1pks.exception.forbidden.AdminSelfAccessChangeException;
import dasein.sem1pks.exception.notfound.AccountNotFoundException;
import dasein.sem1pks.repository.UserRepository;
import dasein.sem1pks.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final RefreshSessionService sessions;
    private final AuditService audit;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<UserView> list(int page, int size) {
        var result = users.findAllByOrderByIdAsc(PageRequest.of(page, size)).map(this::view);
        return new Page<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional
    public UserView create(UUID actor, AdminUserRequest request, String password) {
        User user = new User();
        apply(user, request);
        user.setPasswordHash(passwords.encode(password));
        users.save(user);
        audit.record(actor, "USER_CREATE", "USER", user.getId(), "Пользователь создан");
        return view(user);
    }

    @Transactional
    public UserView update(UUID actor, UUID id, AdminUserRequest request) {
        sessions.lockAccount(id);
        User user = users.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        boolean active = !user.isBlocked();
        if (actor.equals(id) && (request.role() != user.getRole() || request.active() != active)) {
            throw new AdminSelfAccessChangeException();
        }

        apply(user, request);
        users.save(user);
        sessions.revokeUser(id);
        audit.record(actor, "USER_UPDATE", "USER", id, "Пользователь изменен");
        return view(user);
    }

    private void apply(User user, AdminUserRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        users.findByEmail(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new AccountAlreadyExistsException(email);
                });
        user.setUsername(request.username().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setBlocked(!request.active());
    }

    private UserView view(User user) {
        return new UserView(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(), !user.isBlocked());
    }
}
