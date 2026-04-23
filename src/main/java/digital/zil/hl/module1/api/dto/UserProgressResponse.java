package digital.zil.hl.module1.api.dto;

/**
 * Ответ с процентом прогресса по конкретному пользователю.
 */
public record UserProgressResponse(long userId, String login, String email, double progressPercent) {
}
