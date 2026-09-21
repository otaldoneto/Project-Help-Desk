package com.serviceOrder.Management.repositories;

import com.serviceOrder.Management.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // Explicit queries: a derived "existsByCpfOrCnpj" would be parsed by Spring Data as "cpf OR cnpj".
    @Query("select count(c) > 0 from Client c where c.cpfOrCnpj = :cpfOrCnpj")
    boolean existsByDocument(@Param("cpfOrCnpj") String cpfOrCnpj);

    @Query("select count(c) > 0 from Client c where c.cpfOrCnpj = :cpfOrCnpj and c.id <> :id")
    boolean existsByDocumentAndIdNot(@Param("cpfOrCnpj") String cpfOrCnpj, @Param("id") Long id);
}