package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TheaterEventRepository extends JpaRepository<TheaterEvent, Long> {

    List<TheaterEvent> findBySpacecraftId(Long spacecraftId);

    // Eventos de la misma nave que se solapan en rango de fechas y ocurren a la misma hora
    @Query("SELECT e FROM TheaterEvent e WHERE e.spacecraftId = :spacecraftId AND e.time = :time " +
           "AND e.startDate <= :endDate AND e.endDate >= :startDate")
    List<TheaterEvent> findOverlapping(@Param("spacecraftId") Long spacecraftId,
                                        @Param("time") LocalTime time,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // Igual que findOverlapping, pero ignora el propio evento (para validar al editar)
    @Query("SELECT e FROM TheaterEvent e WHERE e.spacecraftId = :spacecraftId AND e.time = :time " +
           "AND e.startDate <= :endDate AND e.endDate >= :startDate AND e.id <> :excludeId")
    List<TheaterEvent> findOverlappingExcludingId(@Param("spacecraftId") Long spacecraftId,
                                        @Param("time") LocalTime time,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("excludeId") Long excludeId);
}