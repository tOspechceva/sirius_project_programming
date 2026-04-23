package digital.zil.hl.module1.repository.jdbc;

import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepositoryAdapter implements UserRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcUserRepositoryAdapter(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User create(final String login, final String email, final LocalDate registrationDate) {
        final String sql = """
                INSERT INTO users (login, email, registration_date)
                VALUES (:login, :email, :registrationDate)
                RETURNING id
                """;
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("login", login)
                .addValue("email", email)
                .addValue("registrationDate", registrationDate);

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        final Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Не удалось получить ID созданного пользователя");
        }
        return new User(id.longValue(), login, email, registrationDate);
    }

    @Override
    public Optional<User> findById(final long id) {
        final String sql = """
                SELECT id, login, email, registration_date
                FROM users
                WHERE id = :id
                """;
        final List<User> users = jdbcTemplate.query(
                sql,
                Map.of("id", id),
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getDate("registration_date").toLocalDate()
                )
        );
        return users.stream().findFirst();
    }

    @Override
    public Optional<User> findByLogin(final String login) {
        final String sql = """
                SELECT id, login, email, registration_date
                FROM users
                WHERE login = :login
                """;
        final List<User> users = jdbcTemplate.query(
                sql,
                Map.of("login", login),
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getDate("registration_date").toLocalDate()
                )
        );
        return users.stream().findFirst();
    }

    @Override
    public Optional<User> update(
            final long id,
            final String login,
            final String email,
            final LocalDate registrationDate
    ) {
        final String sql = """
                UPDATE users
                SET login = :login,
                    email = :email,
                    registration_date = :registrationDate
                WHERE id = :id
                """;
        final int updated = jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("login", login)
                        .addValue("email", email)
                        .addValue("registrationDate", registrationDate)
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return Optional.of(new User(id, login, email, registrationDate));
    }

    @Override
    public boolean deleteById(final long id) {
        final int deleted = jdbcTemplate.update("DELETE FROM users WHERE id = :id", Map.of("id", id));
        return deleted > 0;
    }

    @Override
    public List<User> findAll() {
        final String sql = """
                SELECT id, login, email, registration_date
                FROM users
                ORDER BY id
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getDate("registration_date").toLocalDate()
                )
        );
    }
}
