package br.leetjouney.realtimenotification.service;

import br.leetjouney.realtimenotification.domain.Notification;
import br.leetjouney.realtimenotification.dto.NotificationDTO;
import br.leetjouney.realtimenotification.repository.NotificationRepository;
import br.leetjouney.realtimenotification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public void sendNotification(Long userId, String content) {
        Notification notification = new Notification(userId, content);
        notification = notificationRepository.save(notification);

        NotificationDTO dto = new NotificationDTO(
                notification.getId(),
                notification.getContent(),
                notification.getCreatedAt(),
                notification.isRead()
        );
        // 3. Envio via WebSocket para o usuário específico
        // O destino final será: /user/{username}/queue/notifications
        String username = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                .getUsername();

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                dto
        );
    }

    public List<NotificationDTO> getNotificationsForUser(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDTO(n.getId(), n.getContent(), n.getCreatedAt(), n.isRead()))
                .collect(Collectors.toList());

    }
}
