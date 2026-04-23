package digital.zil.hl.module1.api.dto;

import java.time.LocalDate;

/**
 * Запрос на создание пользователя.
 */
public record CreateUserRequest(String login, String email, LocalDate registrationDate) {
}
