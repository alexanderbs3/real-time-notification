Sistema de Notificações em Tempo Real
📋 Resumo Executivo
Sistema distribuído de notificações em tempo real construído com Spring Boot, WebSocket (STOMP), Apache Kafka e MySQL. Implementa autenticação via JWT e comunicação bidirecional para entrega instantânea de notificações aos usuários conectados.
Casos de uso: dashboards em tempo real, alertas críticos, mensagens instantâneas, atualizações de status, notificações push web.

🏗️ Arquitetura do Sistema
Visão Geral
┌─────────────┐      HTTP/WS        ┌──────────────────┐
│   Cliente   │ ◄──────────────────► │   Spring Boot    │
│  (Browser)  │    JWT + STOMP       │   Application    │
└─────────────┘                      └────────┬─────────┘
│
│ Pub/Sub
│
┌─────────────────▼──────────────┐
│      Apache Kafka              │
│  (Topic: notificacoes-eventos) │
└─────────────────┬──────────────┘
│
│ Consume
┌─────────────────▼──────────────┐
│      MySQL Database            │
│  (Persistência de notificações)│
└────────────────────────────────┘
Fluxo de Notificação

Publicação: API REST recebe requisição → publica no Kafka
Processamento: Consumer do Kafka processa mensagem → persiste no MySQL
Entrega: WebSocket envia notificação para usuário específico conectado
Reconhecimento: Cliente recebe via STOMP e atualiza UI


🚀 Tecnologias e Ferramentas
CategoriaTecnologiaVersãoPropósitoBackendSpring Boot3.xFramework principalSegurançaSpring Security + JWT-Autenticação statelessMensageriaApache Kafka7.5.0Fila distribuídaWebSocketSTOMP/SockJS-Comunicação bidirecionalBanco de DadosMySQL8.0.44PersistênciaOrquestraçãoDocker Compose-Infraestrutura localMonitoramentoKafka UIlatestVisualização de tópicos

📦 Pré-requisitos

Java: JDK 17 ou superior
Maven: 3.8+
Docker: 20.10+ e Docker Compose
IDE: IntelliJ IDEA, Eclipse ou VS Code


⚙️ Configuração e Instalação
1. Clone o Repositório
   bashgit clone <url-do-repositorio>
   cd realtime-notification
2. Inicie a Infraestrutura
   bash# Sobe MySQL, Zookeeper, Kafka e Kafka UI
   docker-compose up -d

# Verifique se os containers estão rodando
docker-compose ps
Healthchecks automáticos:

MySQL: aguarda 5s até aceitar conexões
Zookeeper: porta 2181 operacional
Kafka: dependente do Zookeeper

3. Configure o Projeto
   Edite src/main/resources/application.yml se necessário:
   yamlspring:
   datasource:
   url: jdbc:mysql://localhost:3306/notification_db
   username: user_notification
   password: user_password

kafka:
bootstrap-servers: localhost:9092

application:
security:
jwt:
secret-key: <sua-chave-base64-256bits>
expiration: 86400000  # 24 horas em ms

⚠️ Segurança: Gere uma nova secret-key para produção:
bashopenssl rand -base64 32

4. Compile e Execute
   bash# Compile o projeto
   mvn clean install

# Execute a aplicação
mvn spring-boot:run
A aplicação estará disponível em: http://localhost:8080

🔐 Autenticação JWT
Obter Token
Endpoint: POST /api/auth/login
bashcurl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"username": "user1",
"password": "senha123"
}'
Resposta:
json{
"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
Usuários Pré-cadastrados
UsernamePasswordIDuser1senha1231user2senha1232

💡 Usuários criados automaticamente via DataLoader.java no startup


🌐 API REST
Enviar Notificação de Teste
Endpoint: POST /api/notifications/send-test
bashcurl -X POST http://localhost:8080/api/notifications/send-test \
-H "Content-Type: application/json" \
-H "Authorization: Bearer <seu-token-jwt>" \
-d '{
"userId": 1,
"message": "Nova tarefa atribuída!"
}'
Resposta:
json{
"status": "Sucesso",
"message": "Enviado para o Kafka",
"timestamp": 1734364800000
}
Fluxo Interno:

Controller valida JWT
Publica no Kafka (tópico notificacoes-eventos)
Consumer processa e envia via WebSocket
Cliente recebe notificação em tempo real


🔌 WebSocket (STOMP)
Conexão do Cliente
javascript// 1. Incluir bibliotecas
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

// 2. Conectar ao servidor
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
{ Authorization: `Bearer ${jwtToken}` },
(frame) => {
console.log('Conectado:', frame);

    // 3. Subscrever ao canal de notificações
    stompClient.subscribe('/user/queue/notifications', (message) => {
      const notification = JSON.parse(message.body);
      console.log('Notificação recebida:', notification);
      
      // Atualizar UI
      exibirNotificacao(notification.content);
    });
},
(error) => {
console.error('Erro na conexão:', error);
}
);
Estrutura da Notificação
json{
"id": 42,
"content": "Seu pedido foi aprovado",
"createdAt": "2024-12-16T10:30:00",
"read": false
}

🐳 Docker Compose
Serviços Configurados
yamlservices:
mysql-db:        # Porta 3306
zookeeper:       # Porta 2181
kafka:           # Porta 9092 (externa) / 29092 (interna)
kafka-ui:        # Porta 8089 (http://localhost:8089)
Comandos Úteis
bash# Parar todos os serviços
docker-compose down

# Ver logs em tempo real
docker-compose logs -f kafka

# Reiniciar apenas o Kafka
docker-compose restart kafka

# Limpar volumes (⚠️ apaga dados)
docker-compose down -v
Kafka UI
Acesse http://localhost:8089 para:

Visualizar tópicos e partições
Monitorar mensagens em tempo real
Analisar consumer groups
Inspecionar offsets


📊 Estrutura do Banco de Dados
Tabela: users
ColunaTipoConstraintsidBIGINTPK, AUTO_INCREMENTusernameVARCHAR(255)UNIQUE, NOT NULLpasswordVARCHAR(255)NOT NULL (BCrypt)
Tabela: notificarions (sic - typo no código)
ColunaTipoConstraintsidBIGINTPK, AUTO_INCREMENTuser_idBIGINTNOT NULLcontentTEXTNOT NULLis_readBOOLEANDEFAULT falsecreated_atDATETIMENOT NULL

⚠️ Nota: Corrigir typo notificarions → notifications em Notification.java


🧪 Testando o Sistema
Teste End-to-End

Obter JWT:

bashTOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"user1","password":"senha123"}' \
| jq -r '.token')

Conectar WebSocket (abrir test.html no browser)
Enviar Notificação:

bashcurl -X POST http://localhost:8080/api/notifications/send-test \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"userId":1,"message":"Teste de integração!"}'

Verificar:

Logs do Spring Boot: [KAFKA PUBLISHER] ✅ SUCESSO
Kafka UI: mensagem no tópico notificacoes-eventos
Browser: notificação recebida via WebSocket




🔍 Monitoramento e Logs
Níveis de Log Configurados
yamllogging:
level:
br.leetjouney.realtimenotification: DEBUG
org.hibernate.SQL: DEBUG
org.springframework.web: INFO
Eventos Importantes
log# Conexão WebSocket estabelecida
INFO  WebSocketPresenceEventListener - Nova conexão estabelecida. Usuário: user1

# Mensagem publicada no Kafka
INFO  KafkaNotificationPublisher - ✅ SUCESSO! Offset: 42

# Mensagem consumida
INFO  KafkaNotificationConsumer - Mensagem recebida do Kafka: 1:Nova tarefa

# Notificação enviada via WebSocket
DEBUG NotificationService - Enviando para /user/user1/queue/notifications

⚠️ Troubleshooting
Problema: JWT Inválido
Sintoma: 401 Unauthorized ao enviar notificação
Soluções:

Verificar se token não expirou (24h padrão)
Confirmar header: Authorization: Bearer <token>
Validar secret-key no application.yml

Problema: WebSocket não conecta
Sintoma: Failed to connect to /ws
Soluções:

Verificar CORS no SecurityConfig.corsFilter()
Usar SockJS: new SockJS('/ws') em vez de WebSocket
Confirmar JWT no header STOMP: { Authorization: 'Bearer ...' }

Problema: Kafka não recebe mensagens
Sintoma: Timeout ao publicar
Soluções:
bash# 1. Verificar se Kafka está rodando
docker-compose ps kafka

# 2. Testar conectividade
docker exec -it realtime-notification-kafka \
kafka-topics --bootstrap-server localhost:9092 --list

# 3. Criar tópico manualmente se necessário
docker exec -it realtime-notification-kafka \
kafka-topics --create --topic notificacoes-eventos \
--bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
Problema: MySQL Connection Refused
Sintoma: com.mysql.cj.jdbc.exceptions.CommunicationsException
Soluções:

Aguardar healthcheck: docker-compose logs mysql-db
Verificar porta: netstat -an | grep 3306
Resetar container: docker-compose restart mysql-db


🔒 Considerações de Segurança
✅ Implementado

✅ Senhas hasheadas com BCrypt (custo padrão: 10)
✅ JWT com assinatura HMAC-SHA256
✅ CSRF desabilitado (stateless API)
✅ Autenticação obrigatória para WebSocket

⚠️ Melhorias para Produção

Secret Key: Usar variável de ambiente

yamlapplication:
security:
jwt:
secret-key: ${JWT_SECRET_KEY}

HTTPS: Configurar TLS/SSL

yamlserver:
ssl:
key-store: classpath:keystore.p12
key-store-password: ${KEYSTORE_PASSWORD}

CORS: Restringir origens

javaconfig.setAllowedOrigins(List.of("https://meuapp.com"));

Rate Limiting: Adicionar Spring Cloud Gateway ou Bucket4j
Secrets Management: Integrar com AWS Secrets Manager ou Vault


📈 Escalabilidade
Capacidade Atual

Partições Kafka: 3 (permite 3 consumidores paralelos)
Replicação: 1 (ambiente local)
Conexões WebSocket: Limitado pela JVM heap

Estratégias de Escala

Horizontal: Múltiplas instâncias do Spring Boot

Load Balancer (Nginx/HAProxy)
Session Affinity para WebSocket


Kafka: Aumentar partições e replicação

yamlKAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3

Banco de Dados:

Read Replicas (MySQL Master-Slave)
Sharding por user_id


Cache: Redis para notificações recentes

java@Cacheable(value = "notifications", key = "#userId")
public List<NotificationDTO> getNotificationsForUser(Long userId)

🛠️ Customização
Adicionar Novos Campos à Notificação

Atualizar entidade:

java@Entity
public class Notification {
// ...
@Column
private String priority; // HIGH, MEDIUM, LOW
}

Modificar DTO:

javapublic class NotificationDTO {
// ...
private String priority;
}

Reiniciar (DDL auto-update criará coluna)

Integrar com Sistema Externo
java@Service
public class ExternalSystemPublisher implements NotificationPublisher {
private final RestTemplate restTemplate;

    @Override
    public void publish(Long userId, String content) {
        restTemplate.postForEntity(
            "https://api.externa.com/notify",
            new NotificationRequest(userId, content),
            Void.class
        );
    }
}

📚 Referências

Spring Boot WebSocket Guide
Apache Kafka Documentation
STOMP Protocol Specification
JWT Best Practices


📝 Licença
Este projeto é fornecido como exemplo educacional. Adapte conforme necessário para uso comercial.

👥 Contribuindo

Fork o projeto
Crie uma branch: git checkout -b feature/nova-funcionalidade
Commit: git commit -m 'Adiciona funcionalidade X'
Push: git push origin feature/nova-funcionalidade
Abra um Pull Request


📞 Suporte
Para dúvidas ou problemas:

Verifique a seção Troubleshooting
Consulte os logs: docker-compose logs -f
Abra uma issue no repositório# real-time-notification
