package br.leetjouney.realtimenotification.service;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration

public class KafkaConfig {

    public static final String NOTIFICATION_TOPIC = "notificaoes-eventos";

    /**
     * Configuração programática do tópico.
     * Em ambientes corporativos, usamos partições para permitir
     * que múltiplos consumidores processem mensagens em paralelo.
     */

    @Bean
    public NewTopic notificationTopic(){
        return TopicBuilder.name(NOTIFICATION_TOPIC)
                .partitions(3)// Permite até 3 instâncias de consumidores paralelos
                .replicas(1) // No Docker local usamos 1, em prod seriam 3+
                .build();
    }
}
