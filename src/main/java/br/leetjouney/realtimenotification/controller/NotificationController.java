package br.leetjouney.realtimenotification.controller;

import br.leetjouney.realtimenotification.service.KafkaNotificationPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final KafkaNotificationPublisher publisher;

    public NotificationController(KafkaNotificationPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/send-test")
    public ResponseEntity<?> sendTest(@RequestBody Map<String, Object> request) {
        // FORÇANDO PRINT NO CONSOLE
        System.err.println(">>>> [CONTROLLER] ENTRANDO NO MÉTODO SEND-TEST <<<<<");

        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String message = request.get("message").toString();

            System.err.println(">>>> [CONTROLLER] DADOS: User=" + userId + " Msg=" + message);

            publisher.sendNotification(userId, message);

            return ResponseEntity.ok(Map.of(
                    "status", "Sucesso",
                    "message", "Enviado para o Kafka",
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            System.err.println(">>>> [CONTROLLER] ERRO: " + e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}