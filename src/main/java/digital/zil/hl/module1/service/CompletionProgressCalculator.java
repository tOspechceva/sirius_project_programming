package digital.zil.hl.module1.service;

import digital.zil.hl.module1.model.LessonProgress;
import java.util.Collection;

/**
 * Базовая стратегия расчета прогресса:
 * процент завершенных уроков от общего числа уроков.
 *
 * <p>Если уроков в системе нет, возвращается 0.0, чтобы избежать деления на ноль.
 */
public final class CompletionProgressCalculator implements ProgressCalculator {
    /**
     * @param allLessonsCount количество всех уроков в платформе
     * @param lessonProgresses список прогрессов конкретного пользователя
     * @return процент завершения курса
     */
    @Override
    public double calculatePercent(final int allLessonsCount, final Collection<LessonProgress> lessonProgresses) {
        if (allLessonsCount == 0) {
            return 0.0;
        }

        final long completedLessons = lessonProgresses.stream()
                .filter(LessonProgress::isCompleted)
                .count();

        return (completedLessons * 100.0) / allLessonsCount;
    }
}
