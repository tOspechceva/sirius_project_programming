package digital.zil.hl.module1.api.dto;

/**
 * Запрос на обновление урока.
 */
public record UpdateLessonRequest(
        String topic,
        int videoDurationMinutes,
        String testName,
        int maxTestScore
) {
}
