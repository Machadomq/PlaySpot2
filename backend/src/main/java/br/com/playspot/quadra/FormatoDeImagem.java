package br.com.playspot.quadra;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Identifica o formato pelos primeiros bytes do arquivo, porque o nome e o tipo enviados podem ser falsos. */
enum FormatoDeImagem {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARCA = "WEBP".getBytes(StandardCharsets.US_ASCII);

    final String tipoConteudo;
    final String extensao;

    FormatoDeImagem(String tipoConteudo, String extensao) {
        this.tipoConteudo = tipoConteudo;
        this.extensao = extensao;
    }

    static Optional<FormatoDeImagem> detectar(byte[] bytes) {
        if (comecaCom(bytes, 0, ASSINATURA_JPEG)) return Optional.of(JPEG);
        if (comecaCom(bytes, 0, ASSINATURA_PNG)) return Optional.of(PNG);
        if (comecaCom(bytes, 0, RIFF) && comecaCom(bytes, 8, WEBP_MARCA)) return Optional.of(WEBP);
        return Optional.empty();
    }

    private static boolean comecaCom(byte[] bytes, int posicao, byte[] assinatura) {
        return bytes.length >= posicao + assinatura.length
                && Arrays.equals(bytes, posicao, posicao + assinatura.length, assinatura, 0, assinatura.length);
    }
}
