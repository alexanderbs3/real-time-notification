package br.leetjouney.realtimenotification.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaNotificationPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "notificacoes-eventos";

    public KafkaNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(Long userId, String message) {
        String payload = userId + ":" + message;
        System.out.println("===> [KAFKA PUBLISHER] Tentando enviar: " + payload);

        try {
            kafkaTemplate.send(TOPIC, payload).whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("===> [KAFKA PUBLISHER] ✅ SUCESSO! Offset: " + result.getRecordMetadata().offset());
                } else {
                    System.err.println("===> [KAFKA PUBLISHER] ❌ ERRO: " + ex.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("===> [KAFKA PUBLISHER] ❌ FALHA CRÍTICA: " + e.getMessage());
        }
    }
}