package br.senai.sportfy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "esportes")
public class Esporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "jogadores_sugeridos")
    private Integer jogadoresSugeridos;

    @Column(nullable = false)
    private boolean ativo = true;

    public Esporte() {
    }

    public Esporte(String nome, String descricao, Integer jogadoresSugeridos) {
        this.nome = nome;
        this.descricao = descricao;
        this.jogadoresSugeridos = jogadoresSugeridos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getJogadoresSugeridos() {
        return jogadoresSugeridos;
    }

    public void setJogadoresSugeridos(Integer jogadoresSugeridos) {
        this.jogadoresSugeridos = jogadoresSugeridos;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
