package digital.zil.hl.module1.repository.inmemory;

import digital.zil.hl.module1.model.LessonProgress;
import digital.zil.hl.module1.repository.LessonProgressRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory реализация репозитория прогресса.
 *
 * <p>Ключом выступает строка вида "userId:lessonId", что гарантирует
 * одну запись прогресса на пару пользователь-урок.
 */
public final class StaticLessonProgressRepository implements LessonProgressRepository {
    private static final Map<String, LessonProgress> PROGRESS_BY_USER_AND_LESSON = new LinkedHashMap<>();

    @Override
    public LessonProgress completeLesson(
            final long userId,
            final long lessonId,
            final LocalDate completionDate,
            final int testResult
    ) {
        // Повторное завершение того же урока тем же пользователем перезапишет запись.
        final LessonProgress progress = new LessonProgress(userId, lessonId, completionDate, testResult);
        PROGRESS_BY_USER_AND_LESSON.put(buildKey(userId, lessonId), progress);
        return progress;
    }

    @Override
    public Optional<LessonProgress> findByUserIdAndLessonId(final long userId, final long lessonId) {
        return Optional.ofNullable(PROGRESS_BY_USER_AND_LESSON.get(buildKey(userId, lessonId)));
    }

    @Override
    public List<LessonProgress> findByUserId(final long userId) {
        return PROGRESS_BY_USER_AND_LESSON.values().stream()
                .filter(progress -> progress.userId() == userId)
                .toList();
    }

    @Override
    public List<LessonProgress> findByLessonId(final long lessonId) {
        return PROGRESS_BY_USER_AND_LESSON.values().stream()
                .filter(progress -> progress.lessonId() == lessonId)
                .toList();
    }

    @Override
    public boolean deleteByUserIdAndLessonId(final long userId, final long lessonId) {
        return PROGRESS_BY_USER_AND_LESSON.remove(buildKey(userId, lessonId)) != null;
    }

    @Override
    public int deleteByUserId(final long userId) {
        final List<String> keysToDelete = PROGRESS_BY_USER_AND_LESSON.entrySet().stream()
                .filter(entry -> entry.getValue().userId() == userId)
                .map(Map.Entry::getKey)
                .toList();

        keysToDelete.forEach(PROGRESS_BY_USER_AND_LESSON::remove);
        return keysToDelete.size();
    }

    @Override
    public int deleteByLessonId(final long lessonId) {
        final List<String> keysToDelete = PROGRESS_BY_USER_AND_LESSON.entrySet().stream()
                .filter(entry -> entry.getValue().lessonId() == lessonId)
                .map(Map.Entry::getKey)
                .toList();

        keysToDelete.forEach(PROGRESS_BY_USER_AND_LESSON::remove);
        return keysToDelete.size();
    }

    @Override
    public List<LessonProgress> findAll() {
        return new ArrayList<>(PROGRESS_BY_USER_AND_LESSON.values());
    }

    private static String buildKey(final long userId, final long lessonId) {
        return userId + ":" + lessonId;
    }
}
