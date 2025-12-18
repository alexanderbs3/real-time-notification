package br.leetjouney.realtimenotification.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private boolean read;


}
