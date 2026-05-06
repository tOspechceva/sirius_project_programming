package digital.zil.hl.module1.api;

import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LAB16: принудительное завершение JVM по HTTP (демонстрация отказоустойчивости downstream).
 */
@RestController
@RequestMapping("/api")
public class CrashController {

    @Value("${app.crash.enabled:false}")
    private boolean crashEnabled;

    /**
     * Асинхронно вызывает {@link System#exit(int)}, чтобы клиент успел получить ответ.
     */
    @PostMapping("/crash")
    public ResponseEntity<Void> crash() {
        if (!crashEnabled) {
            return ResponseEntity.notFound().build();
        }
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.exit(1);
        });
        return ResponseEntity.accepted().build();
    }
}
