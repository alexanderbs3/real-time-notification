package br.leetjouney.realtimenotification.config;


import br.leetjouney.realtimenotification.domain.User;
import br.leetjouney.realtimenotification.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);


    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("user1").isEmpty()) {
                User user1 = new User("user1", passwordEncoder.encode("senha123"));
                userRepository.save(user1);
                log.info("Usuário criado: user1 com ID: {}", user1.getId());
            }

            // Cria outro usuário de teste (user2/senha123)
            if (userRepository.findByUsername("user2").isEmpty()) {
                User user2 = new User("user2", passwordEncoder.encode("senha123"));
                userRepository.save(user2);
                log.info("Usuário criado: user2 com ID: {}", user2.getId());
            }
        };

    }
}
