package br.leetjouney.realtimenotification.repository;

import br.leetjouney.realtimenotification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface  NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
