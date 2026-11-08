package br.com.playspot.empresa;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dono_id", nullable = false, updatable = false)
    private Long donoId;

    @Column(nullable = false)
    private String nome;

    private String documento;
    private String telefone;
    private String endereco;
    private String cidade;
    private String uf;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "prazo_cancelamento_horas", nullable = false)
    private int prazoCancelamentoHoras;

    @Column(nullable = false)
    private String fuso = "America/Sao_Paulo";

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    protected Empresa() {
        // exigido pelo JPA
    }

    public Empresa(Long donoId) {
        this.donoId = donoId;
    }

    public void atualizar(EmpresaRequest dados) {
        this.nome = dados.nome().trim();
        this.documento = dados.documento();
        this.telefone = dados.telefone();
        this.endereco = dados.endereco().trim();
        this.cidade = dados.cidade().trim();
        this.uf = dados.uf();
        this.prazoCancelamentoHoras = dados.prazoCancelamentoHoras();
        this.latitude = dados.latitude();
        this.longitude = dados.longitude();
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Long getId() {
        return id;
    }

    public Long getDonoId() {
        return donoId;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public int getPrazoCancelamentoHoras() {
        return prazoCancelamentoHoras;
    }

    public String getFuso() {
        return fuso;
    }
}
