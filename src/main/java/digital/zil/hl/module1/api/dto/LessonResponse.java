package digital.zil.hl.module1.api.dto;

/**
 * Ответ с данными урока.
 */
public record LessonResponse(
        long id,
        String topic,
        int videoDurationMinutes,
        String testName,
        int maxTestScore
) {
}
