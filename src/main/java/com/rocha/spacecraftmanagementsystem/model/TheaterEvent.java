package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

// Fase 2: evento de teatro. Corre todos los dias entre startDate y endDate, siempre a la misma hora.
// Columna "event_time" (no "time") para evitar choques con la palabra reservada de H2.
@Entity
@Table(name = "theater_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spacecraft_id", nullable = false)
    private Long spacecraftId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private TheaterEventType eventType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime time;
}