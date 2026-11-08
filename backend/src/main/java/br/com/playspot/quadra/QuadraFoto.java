package br.com.playspot.quadra;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quadra_foto")
public class QuadraFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quadra_id", nullable = false, updatable = false)
    private Long quadraId;

    @Column(nullable = false, unique = true)
    private String chave;

    @Column(name = "tipo_conteudo", nullable = false)
    private String tipoConteudo;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm = Instant.now();

    protected QuadraFoto() {
        // exigido pelo JPA
    }

    public QuadraFoto(Long quadraId, String chave, String tipoConteudo) {
        this.quadraId = quadraId;
        this.chave = chave;
        this.tipoConteudo = tipoConteudo;
    }

    public Long getId() {
        return id;
    }

    public Long getQuadraId() {
        return quadraId;
    }

    public String getChave() {
        return chave;
    }

    public String getTipoConteudo() {
        return tipoConteudo;
    }
}
