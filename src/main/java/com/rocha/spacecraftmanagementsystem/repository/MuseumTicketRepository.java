package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface MuseumTicketRepository extends JpaRepository<MuseumTicket, Long> {

    Optional<MuseumTicket> findByConfirmationCode(String confirmationCode);

    // Suma de cupos activos (no cancelados) para una nave+dia+hora, usada para validar museumCapacity
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM MuseumTicket t " +
           "WHERE t.spacecraftId = :spacecraftId AND t.visitDate = :visitDate " +
           "AND t.visitTime = :visitTime AND t.status = com.rocha.spacecraftmanagementsystem.model.TicketStatus.ACTIVE")
    Integer sumActiveQuantity(@Param("spacecraftId") Long spacecraftId,
                              @Param("visitDate") LocalDate visitDate,
                              @Param("visitTime") LocalTime visitTime);

    // Fase 3: entradas activas de una nave (sin importar fecha/hora) - se usa al enviarla al
    // taller, para cancelarlas y contar el impacto antes de confirmar.
    List<MuseumTicket> findBySpacecraftIdAndStatus(Long spacecraftId, TicketStatus status);
}