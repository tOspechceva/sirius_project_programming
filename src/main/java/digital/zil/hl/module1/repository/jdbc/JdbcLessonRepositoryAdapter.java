package digital.zil.hl.module1.repository.jdbc;

import digital.zil.hl.module1.model.Lesson;
import digital.zil.hl.module1.model.LessonTest;
import digital.zil.hl.module1.repository.LessonRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLessonRepositoryAdapter implements LessonRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLessonRepositoryAdapter(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Lesson create(
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        final String sql = """
                INSERT INTO lessons (topic, video_duration_minutes, test_name, max_test_score)
                VALUES (:topic, :videoDurationMinutes, :testName, :maxTestScore)
                RETURNING id
                """;
        final MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("topic", topic)
                .addValue("videoDurationMinutes", videoDurationMinutes)
                .addValue("testName", testName)
                .addValue("maxTestScore", maxTestScore);

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        final Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Не удалось получить ID созданного урока");
        }
        return new Lesson(id.longValue(), topic, videoDurationMinutes, new LessonTest(testName, maxTestScore));
    }

    @Override
    public Optional<Lesson> findById(final long id) {
        final String sql = """
                SELECT id, topic, video_duration_minutes, test_name, max_test_score
                FROM lessons
                WHERE id = :id
                """;
        final List<Lesson> lessons = jdbcTemplate.query(
                sql,
                Map.of("id", id),
                (rs, rowNum) -> new Lesson(
                        rs.getLong("id"),
                        rs.getString("topic"),
                        rs.getInt("video_duration_minutes"),
                        new LessonTest(rs.getString("test_name"), rs.getInt("max_test_score"))
                )
        );
        return lessons.stream().findFirst();
    }

    @Override
    public Optional<Lesson> update(
            final long id,
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        final String sql = """
                UPDATE lessons
                SET topic = :topic,
                    video_duration_minutes = :videoDurationMinutes,
                    test_name = :testName,
                    max_test_score = :maxTestScore
                WHERE id = :id
                """;
        final int updated = jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("topic", topic)
                        .addValue("videoDurationMinutes", videoDurationMinutes)
                        .addValue("testName", testName)
                        .addValue("maxTestScore", maxTestScore)
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return Optional.of(new Lesson(id, topic, videoDurationMinutes, new LessonTest(testName, maxTestScore)));
    }

    @Override
    public boolean deleteById(final long id) {
        final int deleted = jdbcTemplate.update("DELETE FROM lessons WHERE id = :id", Map.of("id", id));
        return deleted > 0;
    }

    @Override
    public List<Lesson> findAll() {
        final String sql = """
                SELECT id, topic, video_duration_minutes, test_name, max_test_score
                FROM lessons
                ORDER BY id
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Lesson(
                        rs.getLong("id"),
                        rs.getString("topic"),
                        rs.getInt("video_duration_minutes"),
                        new LessonTest(rs.getString("test_name"), rs.getInt("max_test_score"))
                )
        );
    }
}
