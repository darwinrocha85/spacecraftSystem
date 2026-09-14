package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

// Fase 3: snapshot de una funcion de teatro que se cerro (borro) al enviar la nave al taller.
// Se guarda dentro de RepairRecord para no perder el historial de a cuanto se disrupta cada
// nave, aunque el TheaterEvent original ya no exista.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosedTheaterEvent {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20)
    private TheaterEventType eventType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "event_time")
    private LocalTime eventTime;
}