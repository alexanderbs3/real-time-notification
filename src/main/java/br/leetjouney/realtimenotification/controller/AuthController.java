package br.leetjouney.realtimenotification.controller;

import br.leetjouney.realtimenotification.dto.AuthRequest;
import br.leetjouney.realtimenotification.dto.AuthResponse;
import br.leetjouney.realtimenotification.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")

@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        String token = authService.login(request);
        return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
    }
}
