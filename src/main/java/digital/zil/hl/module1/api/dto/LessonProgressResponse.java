package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Ответ по записи прогресса пользователь-урок.
 */
public record LessonProgressResponse(
        long userId,
        long lessonId,
        LocalDate completionDate,
        int testResult
) {
}
