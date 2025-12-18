package br.leetjouney.realtimenotification.service;

import br.leetjouney.realtimenotification.config.JwtService;
import br.leetjouney.realtimenotification.domain.User;
import br.leetjouney.realtimenotification.dto.AuthRequest;
import br.leetjouney.realtimenotification.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {


    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; // Necessário para a Etapa 6


    /**
     * Processa a requisição de autenticação e gera o token JWT.
     *
     * @param request DTO com username e password
     * @return Token JWT
     */

    public String login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado: " + request.getUsername()));
        return jwtService.generateToken(user);
    }
}
