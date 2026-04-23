package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий пользователей.
 *
 * <p>Интерфейс нужен для отделения бизнес-логики от способа хранения.
 * В текущем проекте используется in-memory реализация на static-коллекциях.
 */
public interface UserRepository {
    /**
     * Создает пользователя.
     */
    User create(String login, String email, LocalDate registrationDate);

    /**
     * Находит пользователя по ID.
     */
    Optional<User> findById(long id);

    /**
     * Находит пользователя по логину.
     */
    Optional<User> findByLogin(String login);

    /**
     * Обновляет пользователя по ID.
     */
    Optional<User> update(long id, String login, String email, LocalDate registrationDate);

    /**
     * Удаляет пользователя по ID.
     */
    boolean deleteById(long id);

    /**
     * Возвращает всех пользователей.
     */
    List<User> findAll();
}
