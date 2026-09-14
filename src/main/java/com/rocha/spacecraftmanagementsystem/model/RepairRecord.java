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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Fase 3: historial de reparaciones de una nave. Se crea al "enviar a taller" (status =
// ENTRO_A_TALLER) y se cierra al "finalizar reparacion" (status = OPERATIVA, finishedAt seteado).
@Entity
@Table(name = "repair_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spacecraft_id", nullable = false)
    private Long spacecraftId;

    @ElementCollection
    @CollectionTable(name = "repair_damages", joinColumns = @JoinColumn(name = "repair_record_id"))
    private List<RepairDamage> damages = new ArrayList<>();

    // Fase 3: snapshot de lo que se cerro para poder enviar esta nave al taller - se llena una
    // sola vez al crear el registro (sendToTaller), antes de borrar los horarios/eventos reales.
    // Sirve para ver, por nave, cuantos horarios/funciones se le han cancelado por reparaciones.
    @ElementCollection
    @CollectionTable(name = "repair_closed_museum_dates", joinColumns = @JoinColumn(name = "repair_record_id"))
    @Column(name = "schedule_date")
    private List<LocalDate> closedMuseumDates = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "repair_closed_theater_events", joinColumns = @JoinColumn(name = "repair_record_id"))
    private List<ClosedTheaterEvent> closedTheaterEvents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SpacecraftStatus status;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "cancelled_ticket_count")
    private Integer cancelledTicketCount;
}