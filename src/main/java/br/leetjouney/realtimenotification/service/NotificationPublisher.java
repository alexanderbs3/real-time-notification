package br.leetjouney.realtimenotification.service;

public interface NotificationPublisher {

    void publish(Long userId,String content);
}
