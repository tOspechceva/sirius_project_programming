package digital.zil.hl.module1.repository.jdbc;

import digital.zil.hl.module1.model.LessonProgress;
import digital.zil.hl.module1.repository.LessonProgressRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLessonProgressRepositoryAdapter implements LessonProgressRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLessonProgressRepositoryAdapter(final NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LessonProgress completeLesson(
            final long userId,
            final long lessonId,
            final LocalDate completionDate,
            final int testResult
    ) {
        final String sql = """
                INSERT INTO lesson_progress (user_id, lesson_id, completion_date, test_result)
                VALUES (:userId, :lessonId, :completionDate, :testResult)
                ON CONFLICT (user_id, lesson_id)
                DO UPDATE SET
                    completion_date = EXCLUDED.completion_date,
                    test_result = EXCLUDED.test_result
                """;
        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("lessonId", lessonId)
                        .addValue("completionDate", completionDate)
                        .addValue("testResult", testResult)
        );
        return new LessonProgress(userId, lessonId, completionDate, testResult);
    }

    @Override
    public Optional<LessonProgress> findByUserIdAndLessonId(final long userId, final long lessonId) {
        final String sql = """
                SELECT user_id, lesson_id, completion_date, test_result
                FROM lesson_progress
                WHERE user_id = :userId AND lesson_id = :lessonId
                """;
        final List<LessonProgress> entries = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("lessonId", lessonId),
                (rs, rowNum) -> new LessonProgress(
                        rs.getLong("user_id"),
                        rs.getLong("lesson_id"),
                        rs.getDate("completion_date").toLocalDate(),
                        rs.getInt("test_result")
                )
        );
        return entries.stream().findFirst();
    }

    @Override
    public List<LessonProgress> findByUserId(final long userId) {
        final String sql = """
                SELECT user_id, lesson_id, completion_date, test_result
                FROM lesson_progress
                WHERE user_id = :userId
                ORDER BY lesson_id
                """;
        return jdbcTemplate.query(
                sql,
                Map.of("userId", userId),
                (rs, rowNum) -> new LessonProgress(
                        rs.getLong("user_id"),
                        rs.getLong("lesson_id"),
                        rs.getDate("completion_date").toLocalDate(),
                        rs.getInt("test_result")
                )
        );
    }

    @Override
    public List<LessonProgress> findByLessonId(final long lessonId) {
        final String sql = """
                SELECT user_id, lesson_id, completion_date, test_result
                FROM lesson_progress
                WHERE lesson_id = :lessonId
                ORDER BY user_id
                """;
        return jdbcTemplate.query(
                sql,
                Map.of("lessonId", lessonId),
                (rs, rowNum) -> new LessonProgress(
                        rs.getLong("user_id"),
                        rs.getLong("lesson_id"),
                        rs.getDate("completion_date").toLocalDate(),
                        rs.getInt("test_result")
                )
        );
    }

    @Override
    public boolean deleteByUserIdAndLessonId(final long userId, final long lessonId) {
        final String sql = """
                DELETE FROM lesson_progress
                WHERE user_id = :userId AND lesson_id = :lessonId
                """;
        final int deleted = jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("lessonId", lessonId)
        );
        return deleted > 0;
    }

    @Override
    public int deleteByUserId(final long userId) {
        final String sql = "DELETE FROM lesson_progress WHERE user_id = :userId";
        return jdbcTemplate.update(sql, Map.of("userId", userId));
    }

    @Override
    public int deleteByLessonId(final long lessonId) {
        final String sql = "DELETE FROM lesson_progress WHERE lesson_id = :lessonId";
        return jdbcTemplate.update(sql, Map.of("lessonId", lessonId));
    }

    @Override
    public List<LessonProgress> findAll() {
        final String sql = """
                SELECT user_id, lesson_id, completion_date, test_result
                FROM lesson_progress
                ORDER BY user_id, lesson_id
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LessonProgress(
                        rs.getLong("user_id"),
                        rs.getLong("lesson_id"),
                        rs.getDate("completion_date").toLocalDate(),
                        rs.getInt("test_result")
                )
        );
    }
}
