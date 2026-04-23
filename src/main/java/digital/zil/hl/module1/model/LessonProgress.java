package digital.zil.hl.module1.model;

import java.time.LocalDate;

/**
 * Прогресс прохождения урока пользователем.
 *
 * <p>Эта сущность реализует связь многие-ко-многим:
 * один пользователь проходит много уроков, и один урок проходят многие пользователи.
 *
 * @param userId идентификатор пользователя
 * @param lessonId идентификатор урока
 * @param completionDate дата завершения урока (null = не завершен)
 * @param testResult набранный балл за тест
 */
public record LessonProgress(long userId, long lessonId, LocalDate completionDate, int testResult) {
    /**
     * @return true, если урок помечен как завершенный.
     */
    public boolean isCompleted() {
        return completionDate != null;
    }
}
