package br.senai.sportfy.domain;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Tabela "locais": o lugar físico (clube, parque, escola) onde ficam as quadras.
 *
 * A coluna "coordenadas" é do tipo point do PostgreSQL e guarda "(longitude,latitude)",
 * na mesma ordem usada por mapas e pelo PostGIS (x = longitude, y = latitude).
 * Para o front/mobile a API devolve latitude e longitude separadas.
 */
@Entity
@Table(name = "locais")
public class Local {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, columnDefinition = "text")
    private String endereco;

    /** Texto no formato "(longitude,latitude)"; convertido de/para point no banco. */
    @Column(name = "coordenadas", columnDefinition = "point")
    @ColumnTransformer(read = "cast(coordenadas as varchar)", write = "cast(? as point)")
    private String coordenadas;

    /** Quem cadastrou o local; só essa pessoa pode alterá-lo. Pode ser vazio (local cadastrado direto no banco). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id")
    private Usuario dono;

    // ------------------------------------------------------------ coordenadas

    @Transient
    public Double getLatitude() {
        double[] xy = lerPonto(coordenadas);
        return xy == null ? null : xy[1];
    }

    @Transient
    public Double getLongitude() {
        double[] xy = lerPonto(coordenadas);
        return xy == null ? null : xy[0];
    }

    public void definirPosicao(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            this.coordenadas = null;
        } else {
            this.coordenadas = String.format(Locale.ROOT, "(%s,%s)", longitude, latitude);
        }
    }

    public boolean temPosicao() {
        return lerPonto(coordenadas) != null;
    }

    /** Lê "(x,y)" devolvido pelo PostgreSQL. Devolve null se vazio ou em formato inesperado. */
    static double[] lerPonto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.replace("(", "").replace(")", "").trim();
        String[] partes = limpo.split(",");
        if (partes.length != 2) {
            return null;
        }
        try {
            return new double[] {Double.parseDouble(partes[0].trim()), Double.parseDouble(partes[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean pertenceA(UUID usuarioId) {
        return dono != null && dono.getId().equals(usuarioId);
    }

    // ------------------------------------------------------------ get/set

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(String coordenadas) {
        this.coordenadas = coordenadas;
    }

    public Usuario getDono() {
        return dono;
    }

    public void setDono(Usuario dono) {
        this.dono = dono;
    }
}
