package br.com.playspot.quadra;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "quadra")
public class Quadra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(name = "duracao_slot_min", nullable = false)
    private int duracaoSlotMin;

    @Column(nullable = false)
    private boolean ativa = true;

    @ManyToMany
    @JoinTable(
            name = "quadra_esporte",
            joinColumns = @JoinColumn(name = "quadra_id"),
            inverseJoinColumns = @JoinColumn(name = "esporte_id"))
    private Set<Esporte> esportes = new HashSet<>();

    protected Quadra() {
        // exigido pelo JPA
    }

    public Quadra(Long empresaId) {
        this.empresaId = empresaId;
    }

    public void atualizar(QuadraRequest dados, List<Esporte> esportes) {
        this.nome = dados.nome().trim();
        this.descricao = dados.descricao();
        this.duracaoSlotMin = dados.duracaoSlotMin();
        this.ativa = dados.ativa();
        this.esportes.clear();
        this.esportes.addAll(esportes);
    }

    public Long getId() {
        return id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDuracaoSlotMin() {
        return duracaoSlotMin;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Set<Esporte> getEsportes() {
        return esportes;
    }
}
