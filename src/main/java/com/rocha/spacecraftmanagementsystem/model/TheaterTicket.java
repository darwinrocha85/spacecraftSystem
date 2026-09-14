package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Fase 2: entrada de teatro. Hasta 5 asientos (1-100) para una funcion puntual (eventId + functionDate).
@Entity
@Table(name = "theater_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "function_date", nullable = false)
    private LocalDate functionDate;

    @ElementCollection
    @CollectionTable(name = "theater_ticket_seats", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "seat_number", nullable = false)
    private List<Integer> seats = new ArrayList<>();

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false, length = 150)
    private String buyerEmail;

    @Column(name = "confirmation_code", nullable = false, unique = true, length = 20)
    private String confirmationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;
}