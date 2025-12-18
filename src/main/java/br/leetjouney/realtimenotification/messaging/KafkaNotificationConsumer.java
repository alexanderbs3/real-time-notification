    package br.leetjouney.realtimenotification.messaging;

    import br.leetjouney.realtimenotification.service.NotificationService;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.kafka.annotation.KafkaListener;
    import org.springframework.stereotype.Component;

    @Component

    public class KafkaNotificationConsumer {


        private static final Logger log = LoggerFactory.getLogger(KafkaNotificationConsumer.class);
        private final NotificationService notificationService;

        public KafkaNotificationConsumer(NotificationService notificationService) {
            this.notificationService = notificationService;
        }

        @KafkaListener(topics = "notificacoes-eventos", groupId = "group_notifications")
        public void consume(String message) {
            log.info("Mensagem recebida do Kafka: {}", message);

            try {
                // Parsing básico da mensagem "userId:content"
                String[] parts = message.split(":", 2);
                Long userId = Long.valueOf(parts[0]);
                String content = parts[1];

                // Entrega final via WebSocket (Reaproveitando o service da Etapa 10)
                notificationService.sendNotification(userId, content);

            } catch (Exception e) {
                log.error("Erro ao processar mensagem do Kafka: {}", e.getMessage());
            }
        }
    }