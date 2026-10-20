package br.senai.sportfy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.senai.sportfy.domain.Local;

public interface LocalRepository extends JpaRepository<Local, UUID> {

    /**
     * Busca por parte do nome ou do endereço (o endereço inclui bairro e cidade). Texto vazio traz todos.
     * Locais privados só aparecem para o dono.
     */
    @Query("""
            select l from Local l
            left join l.dono d
            where (l.publico = true or d.id = :usuarioId)
              and (lower(l.nome) like lower(concat('%', :texto, '%'))
                   or lower(l.endereco) like lower(concat('%', :texto, '%')))
            order by l.nome asc
            """)
    List<Local> buscar(@Param("texto") String texto, @Param("usuarioId") UUID usuarioId);

    @Query("select l from Local l where l.dono.id = :usuarioId order by l.nome asc")
    List<Local> buscarDoDono(@Param("usuarioId") UUID usuarioId);
}
