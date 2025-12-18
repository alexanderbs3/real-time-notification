package br.leetjouney.realtimenotification.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component

public class WebSocketPresenceEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketPresenceEventListener.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = Optional.ofNullable(accessor.getUser())
                .map(p -> p.getName()).orElse("Unknown");
        logger.info("Nova conexão estabelecida. Usuário: {} | SessionId: {}", username, accessor.getSessionId());
    }


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = Optional.ofNullable(accessor.getUser())
                .map(p -> p.getName()).orElse("Unknown");
        logger.info("Conexão encerrada. Usuário: {} | SessionId: {}", username, accessor.getSessionId());
    }



}
