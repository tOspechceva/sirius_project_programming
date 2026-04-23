package digital.zil.hl.module1.service;

import digital.zil.hl.module1.model.LessonProgress;
import java.util.Collection;

/**
 * Контракт стратегии расчета прогресса.
 *
 * <p>Позволяет подменять формулу без изменения сервисов:
 * можно считать прогресс по завершениям, по среднему баллу тестов,
 * по весам уроков и т.д.
 */
public interface ProgressCalculator {
    /**
     * @param allLessonsCount количество доступных уроков в системе
     * @param lessonProgresses прогресс пользователя по урокам
     * @return итоговый прогресс в процентах
     */
    double calculatePercent(int allLessonsCount, Collection<LessonProgress> lessonProgresses);
}
