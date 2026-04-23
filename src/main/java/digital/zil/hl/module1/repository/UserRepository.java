package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий пользователей.
 */
public interface UserRepository {
    User create(String login, String email, LocalDate registrationDate);

    Optional<User> findById(long id);

    Optional<User> findByLogin(String login);

    Optional<User> update(long id, String login, String email, LocalDate registrationDate);

    boolean deleteById(long id);

    List<User> findAll();
}
