package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpacecraftRepository extends JpaRepository<Spacecraft, Long> {
    // Aquí puedes definir métodos de consulta personalizados si lo necesitas
    // Método para buscar naves espaciales por nombre usando 'LIKE' y paginación
    @Query("SELECT s FROM Spacecraft s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Spacecraft> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}

