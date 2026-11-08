package br.com.playspot.comum;

/** Onde os arquivos enviados ficam guardados; o provedor de produção será outra implementação desta interface. */
public interface ArmazenamentoDeArquivos {

    void salvar(String chave, byte[] conteudo);

    byte[] ler(String chave);

    void apagar(String chave);
}
