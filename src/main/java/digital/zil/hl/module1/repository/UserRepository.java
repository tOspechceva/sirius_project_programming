package digital.zil.hl.module1.repository;

import digital.zil.hl.module1.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Репозиторий пользователей.
 */
public interface UserRepository extends CrudRepository<UserEntity, Long> {
    @Override
    List<UserEntity> findAll();

    Optional<UserEntity> findByLogin(String login);
}
