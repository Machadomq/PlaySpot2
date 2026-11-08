package br.com.playspot.comum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Guarda os arquivos numa pasta do servidor. As chaves são geradas pelo sistema, nunca vêm do cliente. */
@Component
public class ArmazenamentoLocal implements ArmazenamentoDeArquivos {

    private final Path diretorio;

    public ArmazenamentoLocal(@Value("${playspot.arquivos.diretorio}") String diretorio) {
        this.diretorio = Path.of(diretorio);
    }

    @Override
    public void salvar(String chave, byte[] conteudo) {
        try {
            Files.createDirectories(diretorio);
            Files.write(diretorio.resolve(chave), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] ler(String chave) {
        try {
            return Files.readAllBytes(diretorio.resolve(chave));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void apagar(String chave) {
        try {
            Files.deleteIfExists(diretorio.resolve(chave));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
