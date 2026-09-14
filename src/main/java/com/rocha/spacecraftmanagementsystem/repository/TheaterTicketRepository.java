package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TheaterTicketRepository extends JpaRepository<TheaterTicket, Long> {

    Optional<TheaterTicket> findByConfirmationCode(String confirmationCode);

    List<TheaterTicket> findByEventIdAndFunctionDateAndStatus(Long eventId, LocalDate functionDate, TicketStatus status);

    // Todas las entradas activas de un evento (sin importar la fecha de funcion) - se usa para
    // proteger editar/borrar un evento que ya tiene entradas vendidas
    List<TheaterTicket> findByEventIdAndStatus(Long eventId, TicketStatus status);
}