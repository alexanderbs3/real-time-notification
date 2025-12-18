    # 🎯 Guia de Entrevista Técnica
## Sistema de Notificações em Tempo Real com Spring Boot + Kafka + WebSocket

---

## 📋 Estrutura da Entrevista

**Duração total**: 60-90 minutos

1. **Arquitetura Geral** (15 min)
2. **Segurança e Autenticação** (20 min)
3. **WebSocket e Comunicação Real-Time** (15 min)
4. **Kafka e Mensageria** (15 min)
5. **Design Patterns e Trade-offs** (15 min)
6. **Cenários de Produção** (10 min)

---

## 🏗️ PARTE 1: Arquitetura Geral (15 min)

### Pergunta 1.1: Visão Macro
**"Explique a arquitetura do sistema de ponta a ponta. Como uma notificação flui desde a criação até a entrega ao usuário?"**

#### ✅ Resposta Esperada (Níveis de Profundidade)

**Nível Júnior**:
```
1. API REST recebe requisição POST /api/notifications/send-test
2. Controller chama service que salva no banco
3. WebSocket envia para o usuário conectado
```

**Nível Pleno**:
```
1. NotificationController valida JWT e recebe payload
2. KafkaNotificationPublisher envia mensagem para tópico Kafka
3. KafkaNotificationConsumer consome mensagem de forma assíncrona
4. NotificationService persiste no MySQL e usa SimpMessagingTemplate
5. WebSocket (STOMP) entrega para /user/{username}/queue/notifications
6. Cliente JavaScript recebe via callback de subscribe()
```

**Nível Sênior**:
```
[Adiciona aos pontos anteriores]

- Menciona padrão pub/sub do Kafka (desacoplamento)
- Explica vantagens de persistir antes do envio (durabilidade)
- Discute garantias de entrega (at-least-once do Kafka)
- Questiona necessidade de DLQ (Dead Letter Queue) para falhas
- Sugere idempotência no consumer (evitar duplicatas)
- Propõe Circuit Breaker se WebSocket falhar
```

#### 🔍 Perguntas de Aprofundamento

**P1**: "Por que usar Kafka? Por que não enviar direto via WebSocket?"

**Resposta Ideal**:
- ✅ **Desacoplamento**: Producer não precisa saber se consumer está online
- ✅ **Resiliência**: Kafka persiste mensagens (retenção configurável)
- ✅ **Escalabilidade**: Múltiplos consumers processam partições em paralelo
- ✅ **Histórico**: Possibilita replay de eventos (auditoria/debug)
- ✅ **Backpressure**: Consumer consome no seu ritmo (não sobrecarrega)

**P2**: "E se o usuário não estiver conectado no WebSocket?"

**Resposta Ideal**:
- ✅ Notificação já está persistida no MySQL (NotificationRepository)
- ✅ Ao conectar, cliente chama GET /api/notifications (endpoint não mostrado)
- ✅ Poderia usar Push Notifications (Firebase/APNs) como fallback
- ✅ Poderia ter flag `delivered: boolean` na entidade

---

### Pergunta 1.2: Tecnologias
**"Justifique a escolha de cada tecnologia. Quais alternativas considerou?"**

#### ✅ Resposta Esperada

| Tecnologia | Justificativa | Alternativas |
|------------|---------------|--------------|
| **Spring Boot** | Ecossistema maduro, integração nativa com WebSocket/Kafka | Quarkus (mais leve), Micronaut (AOT compilation) |
| **JWT** | Stateless, horizontal scaling, mobile-friendly | OAuth2 (mais complexo), Sessions (stateful) |
| **Kafka** | Throughput alto (milhões msg/s), particionamento | RabbitMQ (mais simples), AWS SQS (managed) |
| **WebSocket** | Full-duplex, baixa latência (~5ms) | SSE (unidirecional), Long Polling (overhead) |
| **MySQL** | ACID, relacional, suporta transações | PostgreSQL (JSON), MongoDB (schema-less) |

#### 🎯 Critérios de Avaliação
- ❌ **Vermelho**: Não sabe explicar por que usou tecnologia X
- ⚠️ **Amarelo**: Sabe justificar mas não conhece alternativas
- ✅ **Verde**: Explica trade-offs e quando usar cada opção

---

## 🔐 PARTE 2: Segurança e Autenticação (20 min)

### Pergunta 2.1: Fluxo de Autenticação JWT
**"Detalhe o fluxo desde o login até uma requisição autenticada. Onde o token é validado?"**

#### ✅ Resposta Esperada (Passo a Passo)

```
┌─────────────────────────────────────────────────────────┐
│ FASE 1: LOGIN                                           │
└─────────────────────────────────────────────────────────┘

POST /api/auth/login
Body: { "username": "user1", "password": "senha123" }
         ↓
AuthController.login()
         ↓
AuthService.login() →
    authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken
    )
         ↓
DaoAuthenticationProvider →
    1. UserDetailsService.loadUserByUsername("user1")
    2. PasswordEncoder.matches("senha123", "$2a$10$...")
         ↓ (sucesso)
JwtService.generateToken(userDetails)
    → Claims: { sub: "user1", iat: 1734..., exp: 1735... }
    → Assinatura: HMAC-SHA256(header.payload, secret-key)
         ↓
Response: { "token": "eyJhbGciOiJI..." }


┌─────────────────────────────────────────────────────────┐
│ FASE 2: REQUISIÇÃO AUTENTICADA                          │
└─────────────────────────────────────────────────────────┘

POST /api/notifications/send-test
Header: Authorization: Bearer eyJhbGciOiJI...
         ↓
JwtAuthenticationFilter.doFilterInternal()
         ↓
1. Extrai token do header (substring(7))
2. JwtService.extractUsername(token) → "user1"
3. UserDetailsService.loadUserByUsername("user1")
4. JwtService.isTokenValid(token, userDetails)
    ├─ Verifica assinatura (HMAC)
    ├─ Verifica expiração (exp claim)
    └─ Compara username
         ↓ (válido)
5. Cria UsernamePasswordAuthenticationToken
6. SecurityContextHolder.getContext().setAuthentication(...)
         ↓
FilterChain.doFilter() → Chama Controller
```

#### 🔍 Perguntas de Aprofundamento

**P1**: "O que acontece se alguém alterar o payload do JWT?"

**Resposta Ideal**:
```java
// JWT formato: header.payload.signature

// Payload original:
{ "sub": "user1", "exp": 1735000000 }

// Atacante altera para:
{ "sub": "admin", "exp": 9999999999 }

// Ao validar:
JwtService.extractAllClaims() →
    Jwts.parser().verifyWith(secretKey).build()
    → Assinatura inválida!
    → Lança JwtException
    → Retorna 401 Unauthorized
```

**P2**: "Como proteger a secret-key em produção?"

**Resposta Ideal**:
- ✅ **Variável de ambiente**: `JWT_SECRET_KEY=...` (não commitar)
- ✅ **Secrets Manager**: AWS Secrets Manager, Azure Key Vault
- ✅ **Kubernetes Secrets**: Montado como volume
- ✅ **Rotação**: Renovar chave periodicamente (invalidar tokens antigos)
- ✅ **Comprimento**: Mínimo 256 bits (32 bytes)

**P3**: "Qual o risco de `jwtExpiration: 86400000` (24 horas)?"

**Resposta Ideal**:
- ⚠️ **Risco**: Token roubado é válido por 24h
- ✅ **Mitigação**:
  ```
  - Access Token: 15 minutos (curta duração)
  - Refresh Token: 7 dias (armazenado em httpOnly cookie)
  - Refresh endpoint: /api/auth/refresh
  - Blacklist: Redis com tokens revogados
  ```

---

### Pergunta 2.2: Segurança do WebSocket
**"Como garantir que apenas usuários autenticados conectem no WebSocket?"**

#### ✅ Resposta Esperada

```java
// 1. SecurityConfig permite /ws/** sem autenticação
.requestMatchers("/ws/**").permitAll()
// Por quê? Handshake HTTP não carrega Authorization header no upgrade

// 2. Autenticação real em WebSocketAuthenticationInterceptor
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = ...;
    
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        // Extrai token do header STOMP
        String jwt = accessor.getFirstNativeHeader("Authorization");
        
        // Valida token
        String username = jwtService.extractUsername(jwt);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        
        if (jwtService.isTokenValid(jwt, user)) {
            // Injeta usuário na sessão WebSocket
            accessor.setUser(new UsernamePasswordAuthenticationToken(...));
        } else {
            throw new AuthenticationException("Token inválido");
        }
    }
    return message;
}
```

#### 🔍 Pergunta de Aprofundamento

**P**: "Um atacante pode se conectar ao WebSocket sem token?"

**Resposta Ideal**:
```
✅ Handshake HTTP inicial: SIM (permitAll no SecurityConfig)
❌ Comando CONNECT do STOMP: NÃO (validado no interceptor)

Fluxo:
1. Browser faz HTTP GET /ws (101 Switching Protocols) ← Sucesso
2. Cliente envia frame STOMP:
   CONNECT
   Authorization: Bearer eyJ...
   
3. WebSocketAuthenticationInterceptor valida token
   ↓ (se inválido)
4. Fecha conexão imediatamente (antes de subscrever qualquer canal)
```

**Trade-off**:
```
❌ Validar no HTTP handshake:
   - Problema: JavaScript WebSocket API não suporta custom headers
   - Solução alternativa: Token na query string (inseguro em logs)

✅ Validar no STOMP CONNECT (atual):
   - SockJS permite headers no connect
   - Token não aparece em logs HTTP
```

---

### Pergunta 2.3: BCrypt
**"Por que usar BCrypt? Qual o 'custo' configurado?"**

#### ✅ Resposta Esperada

**Características do BCrypt**:
```java
String hash = passwordEncoder.encode("senha123");
// Resultado: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

Estrutura:
$2a       → Algoritmo (BCrypt)
$10       → Custo (2^10 = 1024 iterações)
$N9qo...  → Salt (22 caracteres, 16 bytes random)
...lhWy   → Hash (31 caracteres, 23 bytes)
```

**Custo Padrão**: 10
```
Tempo de hashing:
- Custo 10: ~100ms
- Custo 12: ~400ms
- Custo 14: ~1600ms

Recomendação:
- Aplicações web: 10-12
- Sistemas críticos: 14+
- Ajustar para ~250ms de tempo de resposta aceitável
```

**Segurança**:
```
✅ Salt automático (previne rainbow tables)
✅ Adaptativo (aumentar custo no futuro)
✅ Resistente a GPUs (memory-hard algorithm)
✅ Comparação constant-time (previne timing attacks)
```

#### 🔍 Pergunta de Aprofundamento

**P**: "Preciso migrar de MD5 para BCrypt. Como fazer sem quebrar logins?"**

**Resposta Ideal**:
```java
@Service
public class MigrationPasswordEncoder implements PasswordEncoder {
    
    @Override
    public String encode(CharSequence rawPassword) {
        return bCryptEncoder.encode(rawPassword);
    }
    
    @Override
    public boolean matches(CharSequence raw, String encoded) {
        // Detecta formato antigo (MD5 = 32 chars hex)
        if (encoded.matches("^[a-f0-9]{32}$")) {
            boolean valid = md5Matches(raw, encoded);
            if (valid) {
                // Migra on-the-fly
                String newHash = bCryptEncoder.encode(raw);
                userRepository.updatePassword(encoded, newHash);
            }
            return valid;
        }
        
        // Formato novo (BCrypt)
        return bCryptEncoder.matches(raw, encoded);
    }
}
```

---

## 🌐 PARTE 3: WebSocket e Comunicação Real-Time (15 min)

### Pergunta 3.1: STOMP vs WebSocket Puro
**"Por que usar STOMP sobre WebSocket? Quais vantagens?"**

#### ✅ Resposta Esperada

**Comparação**:

| Aspecto | WebSocket Puro | STOMP/WebSocket |
|---------|----------------|-----------------|
| **Protocolo** | Frames binários/texto | Frames estruturados |
| **Pub/Sub** | Manual | Nativo (subscribe) |
| **Routing** | Implementar do zero | Prefixos (/app, /topic) |
| **Reconexão** | Manual | SockJS fallback |
| **User Destinations** | Não existe | /user/{name}/queue/... |
| **Headers** | Custom | Padronizados (destination, id) |

**Exemplo WebSocket Puro**:
```javascript
const ws = new WebSocket('ws://localhost:8080/notifications');

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    // Como saber o tipo de mensagem?
    // Como rotear para handlers diferentes?
    // Como garantir ordem de entrega?
};

ws.send(JSON.stringify({ action: 'subscribe', channel: 'user-123' }));
// Servidor precisa parsear e implementar lógica de subscrição
```

**Exemplo STOMP**:
```javascript
stompClient.subscribe('/user/queue/notifications', (message) => {
    const notification = JSON.parse(message.body);
    console.log(notification);
});

stompClient.send('/app/mark-as-read', {}, JSON.stringify({ id: 42 }));
// Servidor mapeia automaticamente para @MessageMapping("/mark-as-read")
```

#### 🔍 Pergunta de Aprofundamento

**P**: "Como funciona o /user prefix internamente?"

**Resposta Ideal**:
```
1. Cliente conecta com JWT
2. WebSocketAuthenticationInterceptor extrai username e injeta:
   accessor.setUser(authenticationToken)

3. Spring mantém SimpUserRegistry:
   {
     "user1": ["session-abc123", "session-xyz789"], // Multi-tab
     "user2": ["session-def456"]
   }

4. Servidor envia:
   messagingTemplate.convertAndSendToUser(
       "user1",
       "/queue/notifications",
       payload
   );

5. UserDestinationMessageHandler resolve:
   /user/user1/queue/notifications
   ↓
   /queue/notifications-userabc123 (sessão 1)
   /queue/notifications-userxyz789 (sessão 2)

6. SimpleBroker entrega para ambas as sessões
```

---

### Pergunta 3.2: SockJS
**"O que é SockJS e por que .withSockJS() é importante?"**

#### ✅ Resposta Esperada

**Problema que Resolve**:
```
Cenários onde WebSocket falha:
- Corporate firewalls bloqueiam porta 80/443 com Upgrade header
- Proxies antigos não entendem protocolo WebSocket
- Redes 3G/4G instáveis (frequentes reconexões)
- Browsers legados (IE < 10)
```

**Estratégia de Fallback**:
```javascript
// Cliente (SockJS automático):
const socket = new SockJS('/ws');

// Tenta em ordem:
1. WebSocket nativo (ws://)
   ↓ (falha: firewall)
2. HTTP Streaming (chunked transfer)
   ↓ (falha: timeout)
3. HTTP Long Polling (request aguarda resposta)
   ↓ (sempre funciona)
```

**Configuração do Servidor**:
```java
registry.addEndpoint("/ws")
        .withSockJS()
        .setStreamBytesLimit(512 * 1024)      // 512KB por stream
        .setHttpMessageCacheSize(1000)        // Cache de mensagens
        .setDisconnectDelay(5 * 1000)         // 5s antes de limpar sessão
        .setHeartbeatTime(25 * 1000);         // Heartbeat a cada 25s
```

**Info Frames (Protocolo SockJS)**:
```
Cliente → GET /ws/123/abc/websocket
Servidor ← o (open frame)

Cliente → ["CONNECT\nAuthorization:Bearer...\n\n\u0000"]
Servidor ← a["CONNECTED\nversion:1.2\n\n\u0000"]

Cliente → ["SEND\ndestination:/app/send\n\nHello\u0000"]
Servidor ← a["MESSAGE\ndestination:/topic/...\n\nWorld\u0000"]

Cliente desconecta
Servidor → c[3000,"Go away!"] (close frame)
```

#### 🔍 Pergunta de Aprofundamento

**P**: "Como testar se o fallback está funcionando?"

**Resposta Ideal**:
```javascript
// Forçar transport específico:
const socket = new SockJS('/ws', null, {
    transports: ['xhr-streaming'] // Não usa WebSocket
});

// Logs do SockJS:
SockJS.debug = true;
// Saída:
// Trying: websocket
// Failed: websocket (WebSocket connection failed)
// Trying: xhr-streaming
// Success: xhr-streaming
```

---

### Pergunta 3.3: Presença de Usuário
**"Como saber quem está online? Implemente um sistema de presença."**

#### ✅ Resposta Esperada (Código)

```java
@Component
public class UserPresenceService {
    
    private final ConcurrentHashMap<String, Set<String>> onlineUsers 
        = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        String username = getUsernameFromEvent(event);
        String sessionId = getSessionId(event);
        
        onlineUsers.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
                   .add(sessionId);
        
        // Notifica outros usuários
        messagingTemplate.convertAndSend("/topic/presence", 
            new PresenceEvent(username, "ONLINE"));
    }
    
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = getUsernameFromEvent(event);
        String sessionId = getSessionId(event);
        
        Set<String> sessions = onlineUsers.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                onlineUsers.remove(username);
                
                // Só notifica quando última sessão desconecta
                messagingTemplate.convertAndSend("/topic/presence",
                    new PresenceEvent(username, "OFFLINE"));
            }
        }
    }
    
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    public Set<String> getOnlineUsers() {
        return onlineUsers.keySet();
    }
}
```

**Cliente**:
```javascript
stompClient.subscribe('/topic/presence', (message) => {
    const event = JSON.parse(message.body);
    updateUserStatus(event.username, event.status);
});
```

---

## 📨 PARTE 4: Kafka e Mensageria (15 min)

### Pergunta 4.1: Por que Kafka?
**"Explique as vantagens do Kafka neste sistema. Quando NÃO usar Kafka?"**

#### ✅ Resposta Esperada

**Vantagens no Contexto**:
```
✅ Desacoplamento Temporal:
   - Producer não espera consumer processar
   - Sistema pode parar e continuar de onde parou

✅ Garantias de Entrega:
   - Mensagens persistidas em disco (log)
   - Retenção configurável (7 dias padrão)
   - Reprocessamento possível (offset reset)

✅ Escalabilidade Horizontal:
   - 3 partições = até 3 consumers paralelos
   - Throughput de 100K+ msg/s por broker

✅ Ordenação por Partição:
   - Mensagens da mesma chave vão para mesma partição
   - Útil para: notificações do mesmo usuário em ordem

✅ Event Sourcing:
   - Histórico completo de eventos
   - Auditoria e debugging
```

**Quando NÃO Usar Kafka**:
```
❌ Baixo volume (<1000 msg/dia):
   - Overhead de infraestrutura não compensa
   - Alternativa: RabbitMQ ou AWS SQS

❌ Request-Response Síncrono:
   - Kafka é async-first
   - Alternativa: REST API ou gRPC

❌ Mensagens grandes (>1MB):
   - Kafka tem limite de 1MB por mensagem (configurável)
   - Alternativa: Armazenar em S3, enviar URL

❌ Priorização de Mensagens:
   - Kafka não tem filas prioritárias nativas
   - Alternativa: RabbitMQ com priority queues

❌ Team sem experiência:
   - Curva de aprendizado íngreme
   - Alternativa: Começar com RabbitMQ (mais simples)
```

---

### Pergunta 4.2: Partições e Consumer Groups
**"O que acontece se eu adicionar mais consumers ao grupo 'group_notifications'?"**

#### ✅ Resposta Esperada

**Configuração Atual**:
```java
@KafkaListener(
    topics = "notificacoes-eventos",
    groupId = "group_notifications"
)
public void consume(String message) { ... }
```

**Cenários**:

**Cenário 1: 1 Consumer, 3 Partições**
```
Partição 0: [msg1, msg2, msg3] ───┐
Partição 1: [msg4, msg5, msg6] ───┼──> Consumer A (processa todas)
Partição 2: [msg7, msg8, msg9] ───┘
```

**Cenário 2: 3 Consumers, 3 Partições** (Ideal)
```
Partição 0: [msg1, msg2, msg3] ──> Consumer A
Partição 1: [msg4, msg5, msg6] ──> Consumer B
Partição 2: [msg7, msg8, msg9] ──> Consumer C

Throughput = 3x (paralelismo máximo)
```

**Cenário 3: 5 Consumers, 3 Partições** (Idle consumers)
```
Partição 0: [msg1, msg2] ──> Consumer A
Partição 1: [msg4, msg5] ──> Consumer B
Partição 2: [msg7, msg8] ──> Consumer C
                             Consumer D (ocioso)
                             Consumer E (ocioso)
```

**Rebalanceamento**:
```
1. Novo consumer entra no grupo
2. Kafka Coordinator pausa todos consumers
3. Reassigna partições (estratégia: RangeAssignor)
4. Consumers retomam processamento

Tempo de rebalance: ~5-10 segundos
Durante rebalance: Processamento pausado
```

#### 🔍 Pergunta de Aprofundamento

**P**: "Como garantir ordem de processamento para mensagens do mesmo usuário?"

**Resposta Ideal**:
```java
// Producer (KafkaNotificationPublisher):
public void sendNotification(Long userId, String message) {
    String payload = userId + ":" + message;
    
    // Enviar com KEY = userId
    kafkaTemplate.send(
        "notificacoes-eventos",
        userId.toString(),  // KEY: garante mesma partição
        payload             // VALUE
    ).whenComplete(...);
}

// Particionamento:
partition = hash(key) % num_partitions
partition = hash("123") % 3 = 2

// Todas mensagens do userId=123 vão para Partição 2
// Um único consumer processa em ordem
```

---

### Pergunta 4.3: Tratamento de Erros
**"O que acontece se o consumer lançar uma exception?"**

#### ✅ Resposta Esperada

**Comportamento Padrão (Problema)**:
```java
@KafkaListener(topics = "notificacoes-eventos", groupId = "group_notifications")
public void consume(String message) {
    try {
        processMessage(message);
    } catch (Exception e) {
        // ⚠️ PROBLEMA: Consumer comita offset automaticamente
        // Mensagem é PERDIDA se não tratar erro
        log.error("Erro ao processar: {}", e.getMessage());
    }
}
```

**Solução 1: Retry com Backoff**
```java
@KafkaListener(topics = "notificacoes-eventos")
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    autoCreateTopics = "true",
    include = {RecoverableException.class}
)
public void consume(String message) {
    processMessage(message); // Pode lançar exception
}

@DltHandler // Dead Letter Topic Handler
public void handleDlt(String message, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error) {
    log.error("Mensagem não processada após 3 tentativas: {}", message);
    // Enviar alerta, salvar em tabela de erros, etc.
}
```

**Solução 2: Manual Acknowledgment**
```java
@KafkaListener(
    topics = "notificacoes-eventos",
    containerFactory = "manualAckContainerFactory"
)
public void consume(String message, Acknowledgment ack) {
    try {
        processMessage(message);
        ack.acknowledge(); // Comita offset manualmente
    } catch (RecoverableException e) {
        // Não comita: mensagem será reprocessada
        log.warn("Tentando novamente...");
    } catch (FatalException e) {
        // Comita para não travar fila
        ack.acknowledge();
        sendToDeadLetter(message, e);
    }
}
```

**Solução 3: Idempotência**
```java
@Transactional
public void processMessage(String message) {
    String[] parts = message.split(":");
    Long userId = Long.valueOf(parts[0]);
    String content = parts[1];
    
    // Gerar ID único baseado na mensagem
    String idempotencyKey = DigestUtils.md5Hex(message);
    
    // Verificar se já processou
    if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
        log.info("Mensagem duplicada, ignorando");
        return;
    }
    
    Notification notification = new Notification(userId, content);
    notification.setIdempotencyKey(idempotencyKey);
    notificationRepository.save(notification);
    
    // Se falhar depois deste ponto e reprocessar, será ignorado
}
```

---

## 🎨 PARTE 5: Design Patterns e Trade-offs (15 min)

### Pergunta 5.1: Padrões Identificados
**"Quais design patterns você identifica no código? Justifique cada um."**

#### ✅ Resposta Esperada

**1. Dependency Injection (DI)**
```java
// ApplicationConfig.java
public ApplicationConfig(UserRepository userRepository) {
    this.userRepository = userRepository; // Injetado pelo Spring
}

Vantagens:
✅ Testabilidade (mock dependencies)
✅ Loose coupling (trocar implementações)
✅ Inversão de controle (IoC)
```

**2. Builder Pattern**
```java
// JwtService.java
return Jwts.builder()
    .claims(extraClaims)
    .subject(username)
    .issuedAt(new Date())
    .expiration(new Date())
    .signWith(key)
    .compact();

Vantagens:
✅ Fluent API (legível)
✅ Parâmetros opcionais
✅ Imutabilidade (thread-safe)
```

**3. Strategy Pattern**
```java
// AuthenticationProvider (interface)
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    // Poderia trocar por LdapAuthenticationProvider sem mudar código
    return authProvider;
}

Vantagens:
✅ Trocar algoritmo de autenticação em runtime
✅ Suporta múltiplas estratégias (LDAP + DB)
```

**4. Observer Pattern**
```java
// WebSocketPresenceEventListener.java
@EventListener
public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    // Reage a eventos sem acoplamento direto
}

Vantagens:
✅ Desacoplamento (publisher não conhece subscribers)