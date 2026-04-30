package digital.zil.hl.module1.api;

import digital.zil.hl.module1.api.dto.CreateUserRequest;
import digital.zil.hl.module1.api.dto.UpdateUserRequest;
import digital.zil.hl.module1.api.dto.UserResponse;
import digital.zil.hl.module1.entity.UserEntity;
import digital.zil.hl.module1.model.User;
import digital.zil.hl.module1.repository.UserRepository;
import digital.zil.hl.module1.service.CourseProgressService;
import digital.zil.hl.module1.service.ObservabilityService;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API для работы с пользователями.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final CourseProgressService courseProgressService;
    private final ObservabilityService observabilityService;

    public UserController(
            final UserRepository userRepository,
            final CourseProgressService courseProgressService,
            final ObservabilityService observabilityService
    ) {
        this.userRepository = userRepository;
        this.courseProgressService = courseProgressService;
        this.observabilityService = observabilityService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody final CreateUserRequest request) {
        return timed("controller:createUser", () -> {
            final UserEntity entity = new UserEntity();
            entity.setLogin(request.login());
            entity.setEmail(request.email());
            entity.setRegistrationDate(request.registrationDate());
            final UserEntity created = userRepository.save(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(toDomain(created)));
        });
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return timed("controller:getAllUsers", () -> userRepository.findAll().stream()
                .map(UserController::toDomain)
                .map(UserController::toUserResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable final long id) {
        return timed("controller:getUserById", () -> {
            final User user = userRepository.findById(id)
                    .map(UserController::toDomain)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + id));
            return toUserResponse(user);
        });
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable final long id,
            @RequestBody final UpdateUserRequest request
    ) {
        return timed("controller:updateUser", () -> {
            final UserEntity updated = userRepository.findById(id)
                    .map(entity -> {
                        entity.setLogin(request.login());
                        entity.setEmail(request.email());
                        entity.setRegistrationDate(request.registrationDate());
                        return userRepository.save(entity);
                    })
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + id));
            return toUserResponse(toDomain(updated));
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable final long id) {
        return timed("controller:deleteUser", () -> {
            courseProgressService.deleteAllProgressForUser(id);
            if (!userRepository.existsById(id)) {
                throw new IllegalArgumentException("Пользователь не найден: " + id);
            }
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        });
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearUsers() {
        return timed("controller:clearUsers", () -> {
            userRepository.findAll().forEach(user -> courseProgressService.deleteAllProgressForUser(user.getId()));
            userRepository.deleteAll();
            return ResponseEntity.noContent().build();
        });
    }

    private <T> T timed(final String operation, final Supplier<T> supplier) {
        final long started = System.nanoTime();
        try {
            final T result = supplier.get();
            observabilityService.recordSuccess(operation, System.nanoTime() - started);
            return result;
        } catch (RuntimeException ex) {
            observabilityService.recordFailure(operation, System.nanoTime() - started);
            throw ex;
        }
    }

    private static User toDomain(final UserEntity entity) {
        return new User(entity.getId(), entity.getLogin(), entity.getEmail(), entity.getRegistrationDate());
    }

    private static UserResponse toUserResponse(final User user) {
        return new UserResponse(user.id(), user.login(), user.email(), user.registrationDate());
    }
}
