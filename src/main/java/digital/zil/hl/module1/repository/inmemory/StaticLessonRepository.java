package digital.zil.hl.module1.repository.inmemory;

import digital.zil.hl.module1.model.Lesson;
import digital.zil.hl.module1.model.LessonTest;
import digital.zil.hl.module1.repository.LessonRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory реализация репозитория уроков.
 *
 * <p>Сохраняет уроки в static-структуре, имитируя простое хранилище
 * без подключения БД.
 */
public final class StaticLessonRepository implements LessonRepository {
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);
    private static final Map<Long, Lesson> LESSONS = new LinkedHashMap<>();

    @Override
    public Lesson create(
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        final long id = ID_SEQUENCE.getAndIncrement();
        final Lesson lesson = new Lesson(
                id,
                topic,
                videoDurationMinutes,
                new LessonTest(testName, maxTestScore)
        );
        LESSONS.put(id, lesson);
        return lesson;
    }

    @Override
    public Optional<Lesson> findById(final long id) {
        return Optional.ofNullable(LESSONS.get(id));
    }

    @Override
    public Optional<Lesson> update(
            final long id,
            final String topic,
            final int videoDurationMinutes,
            final String testName,
            final int maxTestScore
    ) {
        if (!LESSONS.containsKey(id)) {
            return Optional.empty();
        }

        final Lesson updated = new Lesson(
                id,
                topic,
                videoDurationMinutes,
                new LessonTest(testName, maxTestScore)
        );
        LESSONS.put(id, updated);
        return Optional.of(updated);
    }

    @Override
    public boolean deleteById(final long id) {
        return LESSONS.remove(id) != null;
    }

    @Override
    public List<Lesson> findAll() {
        return new ArrayList<>(LESSONS.values());
    }
}
