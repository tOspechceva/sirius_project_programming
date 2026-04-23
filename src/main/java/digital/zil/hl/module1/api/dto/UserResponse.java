package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Ответ с данными пользователя.
 */
public record UserResponse(long id, String login, String email, LocalDate registrationDate) {
}
