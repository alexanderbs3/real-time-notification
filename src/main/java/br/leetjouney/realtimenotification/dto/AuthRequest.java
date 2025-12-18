package br.leetjouney.realtimenotification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {

    @NotBlank(message = "username é obrigatório")
    private String username;

    @NotBlank(message = "Password é obrigatório")
    private String password;


}
