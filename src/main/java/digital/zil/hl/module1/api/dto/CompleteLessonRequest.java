package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Запрос на отметку завершения урока.
 */
public record CompleteLessonRequest(
        long userId,
        long lessonId,
        LocalDate completionDate,
        int testResult
) {
}
