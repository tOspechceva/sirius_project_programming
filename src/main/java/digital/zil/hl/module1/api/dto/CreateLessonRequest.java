package digital.zil.hl.module1.api.dto;

/**
 * Запрос на создание урока.
 */
public record CreateLessonRequest(
        String topic,
        int videoDurationMinutes,
        String testName,
        int maxTestScore
) {
}
