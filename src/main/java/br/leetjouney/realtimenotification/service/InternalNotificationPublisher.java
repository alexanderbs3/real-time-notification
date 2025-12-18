package br.leetjouney.realtimenotification.service;


import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.pl.REGON;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalNotificationPublisher implements NotificationPublisher{

    private final NotificationService notificationService;


    @Override
    public void publish(Long userId, String content) {
        notificationService.sendNotification(userId,content);

    }
}
