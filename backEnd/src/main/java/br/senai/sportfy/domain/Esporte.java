package br.senai.sportfy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabela "esportes" (cadastrada pelo responsável do banco; a API só lê). */
@Entity
@Table(name = "esportes")
public class Esporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "quantidade_jogadores_padrao")
    private Integer quantidadeJogadoresPadrao;

    public Esporte() {
    }

    public Esporte(Integer id, String nome, Integer quantidadeJogadoresPadrao) {
        this.id = id;
        this.nome = nome;
        this.quantidadeJogadoresPadrao = quantidadeJogadoresPadrao;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getQuantidadeJogadoresPadrao() {
        return quantidadeJogadoresPadrao;
    }

    public void setQuantidadeJogadoresPadrao(Integer quantidadeJogadoresPadrao) {
        this.quantidadeJogadoresPadrao = quantidadeJogadoresPadrao;
    }
}
