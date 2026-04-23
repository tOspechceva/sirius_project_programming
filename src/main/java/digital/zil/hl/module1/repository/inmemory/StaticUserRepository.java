package digital.zil.hl.module1.repository.inmemory;

import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory реализация репозитория пользователей.
 *
 * <p>Использует static-коллекцию, поэтому данные живут в рамках процесса приложения
 * и доступны всем экземплярам этого класса.
 */
public final class StaticUserRepository implements UserRepository {
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);
    private static final Map<Long, User> USERS = new LinkedHashMap<>();

    @Override
    public User create(final String login, final String email, final LocalDate registrationDate) {
        final long id = ID_SEQUENCE.getAndIncrement();
        final User user = new User(id, login, email, registrationDate);
        USERS.put(id, user);
        return user;
    }

    @Override
    public Optional<User> findById(final long id) {
        return Optional.ofNullable(USERS.get(id));
    }

    @Override
    public Optional<User> findByLogin(final String login) {
        return USERS.values().stream()
                .filter(user -> user.login().equals(login))
                .findFirst();
    }

    @Override
    public Optional<User> update(
            final long id,
            final String login,
            final String email,
            final LocalDate registrationDate
    ) {
        if (!USERS.containsKey(id)) {
            return Optional.empty();
        }

        final User updated = new User(id, login, email, registrationDate);
        USERS.put(id, updated);
        return Optional.of(updated);
    }

    @Override
    public boolean deleteById(final long id) {
        return USERS.remove(id) != null;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(USERS.values());
    }
}
