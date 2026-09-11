package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpacecraftRepository extends JpaRepository<Spacecraft, Long> {

    // Metodo para buscar naves espaciales por nombre usando 'LIKE' y paginacion
    @Query("SELECT s FROM Spacecraft s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Spacecraft> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    // Naves habilitadas como recinto (museo y/o teatro), usadas por la Fase 2
    List<Spacecraft> findByIsMuseumTrue();

    List<Spacecraft> findByIsTheaterTrue();

    List<Spacecraft> findByIsMuseumTrueOrIsTheaterTrue();
}