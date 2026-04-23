package digital.zil.hl.module1.model;

import java.time.LocalDate;

/**
 * Пользователь платформы онлайн-курсов.
 *
 * @param id внутренний идентификатор
 * @param login уникальный логин ученика
 * @param email контактный email
 * @param registrationDate дата регистрации в системе
 */
public record User(long id, String login, String email, LocalDate registrationDate) {
}
