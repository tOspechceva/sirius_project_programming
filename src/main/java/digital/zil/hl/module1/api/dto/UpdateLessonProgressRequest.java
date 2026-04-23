package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Запрос на обновление прогресса по выбранному уроку.
 */
public record UpdateLessonProgressRequest(LocalDate completionDate, int testResult) {
}
