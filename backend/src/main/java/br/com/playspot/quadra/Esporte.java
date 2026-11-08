package br.com.playspot.quadra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "esporte")
public class Esporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true)
    private String nome;

    protected Esporte() {
        // exigido pelo JPA
    }

    public Short getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
