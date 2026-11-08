package br.com.playspot.auth;

public record LoginResponse(String token, String tipo, long expiraEmSegundos, UsuarioResponse usuario) {
}
