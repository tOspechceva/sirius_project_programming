package digital.zil.hl.module1.repository.jpa;

import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.repository.jpa.entity.UserEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserEntityRepository userEntityRepository;

    public JpaUserRepositoryAdapter(final SpringDataUserEntityRepository userEntityRepository) {
        this.userEntityRepository = userEntityRepository;
    }

    @Override
    @Transactional
    public User create(final String login, final String email, final LocalDate registrationDate) {
        final UserEntity entity = new UserEntity();
        entity.setLogin(login);
        entity.setEmail(email);
        entity.setRegistrationDate(registrationDate);
        return toDomain(userEntityRepository.save(entity));
    }

    @Override
    public Optional<User> findById(final long id) {
        return userEntityRepository.findById(id).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByLogin(final String login) {
        return userEntityRepository.findByLogin(login).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<User> update(
            final long id,
            final String login,
            final String email,
            final LocalDate registrationDate
    ) {
        return userEntityRepository.findById(id)
                .map(entity -> {
                    entity.setLogin(login);
                    entity.setEmail(email);
                    entity.setRegistrationDate(registrationDate);
                    return toDomain(userEntityRepository.save(entity));
                });
    }

    @Override
    @Transactional
    public boolean deleteById(final long id) {
        if (!userEntityRepository.existsById(id)) {
            return false;
        }
        userEntityRepository.deleteById(id);
        return true;
    }

    @Override
    public List<User> findAll() {
        return userEntityRepository.findAll().stream()
                .map(JpaUserRepositoryAdapter::toDomain)
                .toList();
    }

    private static User toDomain(final UserEntity entity) {
        return new User(entity.getId(), entity.getLogin(), entity.getEmail(), entity.getRegistrationDate());
    }
}
