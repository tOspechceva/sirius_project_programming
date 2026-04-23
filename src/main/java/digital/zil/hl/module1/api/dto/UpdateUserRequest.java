package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Запрос на обновление пользователя.
 */
public record UpdateUserRequest(String login, String email, LocalDate registrationDate) {
}
