# Fase 3 - taller de reparacion: escribe/actualiza los archivos Java del backend.
# Corre esto desde cualquier lado (usa rutas absolutas). Requiere que el repo
# spacecraftSystem ya exista en la ruta de abajo.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File "C:\ruta\a\fase3-backend.ps1"

$ErrorActionPreference = "Stop"

$repoRoot = "C:\Users\sebas\Documents\Projects\spacecraftSystem"
$base = Join-Path $repoRoot "src\main\java\com\rocha\spacecraftmanagementsystem"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

if (-not (Test-Path $repoRoot)) {
    Write-Host "No encuentro el repo en $repoRoot - ajusta `$repoRoot al inicio del script." -ForegroundColor Red
    exit 1
}

function Write-CodeFile {
    param([string]$RelativePath, [string]$Content)
    $fullPath = Join-Path $base $RelativePath
    $dir = Split-Path $fullPath -Parent
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($fullPath, $Content, $utf8NoBom)
    Write-Host "  escrito: $RelativePath"
}

Write-Host "Escribiendo archivos de Fase 3 en $base ..." -ForegroundColor Cyan

# ---------- model/SpacecraftStatus.java (nuevo) ----------
Write-CodeFile "model\SpacecraftStatus.java" @'
package com.rocha.spacecraftmanagementsystem.model;

// Fase 3: estado operativo de la nave. OPERATIVA es el default; los demas son sub-estados
// dentro del taller de reparacion (se avanzan manualmente desde el admin).
public enum SpacecraftStatus {
    OPERATIVA,
    ENTRO_A_TALLER,
    EN_REVISION,
    ESPERA_REPUESTOS,
    EN_PROCESO
}
'@

# ---------- model/Spacecraft.java (se sobreescribe: se agrega el campo status) ----------
Write-CodeFile "model\Spacecraft.java" @'
package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "spacecrafts") // Nombre de la tabla en la base de datos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Spacecraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100) // Nombre de la columna, longitud maxima
    private String name;

    @Column(name = "franchise", nullable = false, length = 50) // Serie o pelicula a la que pertenece
    private String franchise;

    @Column(name = "crew_capacity") // Capacidad de tripulacion
    private Integer crewCapacity;

    @Column(name = "speed") // Velocidad maxima en alguna unidad
    private Double speed;

    @Column(name = "spacecraft_type", length = 50) // Tipo de nave
    private String spacecraftType;

    @Column(name = "is_armed") // Indica si esta equipada con armas
    private Boolean isArmed;

    @Column(name = "is_museum") // Indica si la nave funciona como museo
    private Boolean isMuseum;

    @Column(name = "is_theater") // Indica si la nave funciona como teatro
    private Boolean isTheater;

    @Column(name = "museum_capacity") // Maximo de personas dentro del museo al mismo tiempo (obligatorio si isMuseum = true)
    private Integer museumCapacity;

    // Fase 3: estado operativo / taller de reparacion. Default OPERATIVA para naves nuevas.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private SpacecraftStatus status = SpacecraftStatus.OPERATIVA;
}
'@

# ---------- model/DamageCategory.java (nuevo) ----------
Write-CodeFile "model\DamageCategory.java" @'
package com.rocha.spacecraftmanagementsystem.model;

import java.util.List;

// Fase 3: taxonomia de danos para el taller (cascada categoria -> subtipo en el frontend).
// Fuente unica de verdad: el frontend la consume via GET /api/repairs/damage-catalog.
public enum DamageCategory {

    CASCO_ESTRUCTURA("Casco y Estructura", List.of(
            "Abolladura", "Grieta", "Perforacion", "Dano termico")),
    PROPULSION("Propulsion", List.of(
            "Motor principal", "Propulsores RCS", "Sistema de combustible", "Sobrecalentamiento")),
    ELECTRICO_AVIONICA("Electrico y Avionica", List.of(
            "Cableado", "Computadora de vuelo", "Sensores", "Falla de energia/baterias")),
    SOPORTE_VITAL("Soporte Vital", List.of(
            "Oxigeno", "Presurizacion", "Control de temperatura")),
    COMUNICACIONES("Comunicaciones", List.of(
            "Antena/transmisor", "Sistema de navegacion"));

    private final String label;
    private final List<String> subtypes;

    DamageCategory(String label, List<String> subtypes) {
        this.label = label;
        this.subtypes = subtypes;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getSubtypes() {
        return subtypes;
    }
}
'@

# ---------- model/ClosedTheaterEvent.java (nuevo) ----------
Write-CodeFile "model\ClosedTheaterEvent.java" @'
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
'@

# ---------- model/RepairDamage.java (nuevo) ----------
Write-CodeFile "model\RepairDamage.java" @'
package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fase 3: un dano puntual (categoria + subtipo) dentro de un RepairRecord.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairDamage {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private DamageCategory category;

    @Column(name = "subtype", nullable = false, length = 60)
    private String subtype;
}
'@

# ---------- model/RepairRecord.java (nuevo) ----------
Write-CodeFile "model\RepairRecord.java" @'
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
'@

# ---------- repository/RepairRecordRepository.java (nuevo) ----------
Write-CodeFile "repository\RepairRecordRepository.java" @'
package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {

    List<RepairRecord> findBySpacecraftIdOrderBySentAtDesc(Long spacecraftId);

    // El registro abierto (sin finishedAt) de una nave, si esta en el taller
    Optional<RepairRecord> findBySpacecraftIdAndFinishedAtIsNull(Long spacecraftId);
}
'@

# ---------- repository/MuseumTicketRepository.java (se sobreescribe: se agrega findBySpacecraftIdAndStatus) ----------
Write-CodeFile "repository\MuseumTicketRepository.java" @'
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
'@

# ---------- service/MuseumScheduleService.java (se sobreescribe: bloquea altas si la nave esta en reparacion) ----------
Write-CodeFile "service\MuseumScheduleService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.repository.MuseumScheduleRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MuseumScheduleService {

    private static final int WINDOW_DAYS = 8; // hoy inclusive + 7 dias mas

    @Autowired
    private MuseumScheduleRepository museumScheduleRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    public MuseumSchedule saveSchedule(MuseumSchedule schedule) {
        Spacecraft spacecraft = spacecraftRepository.findById(schedule.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException(
                        "Spacecraft with ID " + schedule.getSpacecraftId() + " not found"));

        if (!Boolean.TRUE.equals(spacecraft.getIsMuseum())) {
            throw new IllegalArgumentException("Spacecraft " + spacecraft.getId() + " is not a museum");
        }

        // Fase 3: una nave en el taller no puede abrir nuevos horarios de museo
        if (spacecraft.getStatus() != null && spacecraft.getStatus() != SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException(
                    "Spacecraft " + spacecraft.getId() + " is en reparacion (" + spacecraft.getStatus()
                            + ") and cannot have new museum schedules");
        }

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(WINDOW_DAYS - 1);
        if (schedule.getDate() == null || schedule.getDate().isBefore(today) || schedule.getDate().isAfter(limit)) {
            throw new IllegalArgumentException(
                    "date must be between " + today + " and " + limit + " (today plus " + (WINDOW_DAYS - 1) + " days)");
        }

        if (schedule.getOpenTime() == null || schedule.getCloseTime() == null
                || !schedule.getCloseTime().isAfter(schedule.getOpenTime())) {
            throw new IllegalArgumentException("closeTime must be after openTime");
        }

        return museumScheduleRepository.findBySpacecraftIdAndDate(schedule.getSpacecraftId(), schedule.getDate())
                .map(existing -> {
                    existing.setOpenTime(schedule.getOpenTime());
                    existing.setCloseTime(schedule.getCloseTime());
                    return museumScheduleRepository.save(existing);
                })
                .orElseGet(() -> museumScheduleRepository.save(schedule));
    }

    public List<MuseumSchedule> getSchedule(Long spacecraftId) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(WINDOW_DAYS - 1);
        return museumScheduleRepository.findBySpacecraftIdOrderByDateAsc(spacecraftId).stream()
                .filter(s -> !s.getDate().isBefore(today) && !s.getDate().isAfter(limit))
                .toList();
    }
}
'@

# ---------- service/TheaterEventService.java (se sobreescribe: bloquea create() si la nave esta en reparacion) ----------
Write-CodeFile "service\TheaterEventService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterEventRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class TheaterEventService {

    // Constante del sistema: cada funcion tiene siempre 100 asientos (no es un campo editable)
    public static final int SEATS_PER_FUNCTION = 100;

    @Autowired
    private TheaterEventRepository theaterEventRepository;

    @Autowired
    private TheaterTicketRepository theaterTicketRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    public TheaterEvent create(TheaterEvent event) {
        Spacecraft spacecraft = spacecraftRepository.findById(event.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + event.getSpacecraftId() + " not found"));

        if (!Boolean.TRUE.equals(spacecraft.getIsTheater())) {
            throw new IllegalArgumentException("Spacecraft " + spacecraft.getId() + " is not a theater");
        }

        // Fase 3: una nave en el taller no puede abrir nuevas funciones de teatro
        if (spacecraft.getStatus() != null && spacecraft.getStatus() != SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException(
                    "Spacecraft " + spacecraft.getId() + " is en reparacion (" + spacecraft.getStatus()
                            + ") and cannot have new theater events");
        }

        if (event.getStartDate() == null || event.getEndDate() == null || event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        if (event.getTime() == null) {
            throw new IllegalArgumentException("time is required");
        }

        if (event.getEventType() == null) {
            throw new IllegalArgumentException("eventType is required");
        }

        List<TheaterEvent> overlapping = theaterEventRepository.findOverlapping(
                event.getSpacecraftId(), event.getTime(), event.getStartDate(), event.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "Spacecraft " + spacecraft.getId() + " already has an event at " + event.getTime()
                            + " overlapping that date range");
        }

        event.setId(null);
        return theaterEventRepository.save(event);
    }

    // Edita tipo/horario/rango de fechas de un evento existente. No se puede dejar fuera del
    // nuevo rango una funcion que ya tenga entradas activas vendidas, ni chocar con otro evento.
    public TheaterEvent update(Long id, TheaterEvent updated) {
        TheaterEvent event = getById(id);

        if (updated.getStartDate() == null || updated.getEndDate() == null || updated.getEndDate().isBefore(updated.getStartDate())) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        if (updated.getTime() == null) {
            throw new IllegalArgumentException("time is required");
        }

        if (updated.getEventType() == null) {
            throw new IllegalArgumentException("eventType is required");
        }

        List<TheaterEvent> overlapping = theaterEventRepository.findOverlappingExcludingId(
                event.getSpacecraftId(), updated.getTime(), updated.getStartDate(), updated.getEndDate(), id);
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "Spacecraft " + event.getSpacecraftId() + " already has another event at " + updated.getTime()
                            + " overlapping that date range");
        }

        List<TheaterTicket> activeTickets = theaterTicketRepository.findByEventIdAndStatus(id, TicketStatus.ACTIVE);
        for (TheaterTicket t : activeTickets) {
            if (t.getFunctionDate().isBefore(updated.getStartDate()) || t.getFunctionDate().isAfter(updated.getEndDate())) {
                throw new IllegalArgumentException(
                        "Cannot update: there is an active ticket sold for " + t.getFunctionDate()
                                + ", which would fall outside the new date range");
            }
        }

        event.setEventType(updated.getEventType());
        event.setStartDate(updated.getStartDate());
        event.setEndDate(updated.getEndDate());
        event.setTime(updated.getTime());
        return theaterEventRepository.save(event);
    }

    // Borra un evento solo si no tiene entradas activas vendidas
    public void delete(Long id) {
        TheaterEvent event = getById(id);
        List<TheaterTicket> activeTickets = theaterTicketRepository.findByEventIdAndStatus(id, TicketStatus.ACTIVE);
        if (!activeTickets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete event " + id + ": it has " + activeTickets.size() + " active ticket(s) sold");
        }
        theaterEventRepository.delete(event);
    }

    // Resumen de ventas de un evento: total de asientos vendidos y el desglose por fecha de funcion
    public Map<String, Object> getSales(Long id) {
        getById(id); // valida que el evento exista

        List<TheaterTicket> tickets = theaterTicketRepository.findByEventIdAndStatus(id, TicketStatus.ACTIVE);
        Map<LocalDate, Integer> byDate = new TreeMap<>();
        int total = 0;
        for (TheaterTicket t : tickets) {
            int seats = t.getSeats() == null ? 0 : t.getSeats().size();
            total += seats;
            byDate.merge(t.getFunctionDate(), seats, Integer::sum);
        }

        List<Map<String, Object>> byDateList = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : byDate.entrySet()) {
            byDateList.add(Map.of(
                    "functionDate", entry.getKey().toString(),
                    "seatsSold", entry.getValue(),
                    "seatsPerFunction", SEATS_PER_FUNCTION
            ));
        }

        return Map.of(
                "eventId", id,
                "totalSeatsSold", total,
                "activeTicketCount", tickets.size(),
                "seatsPerFunction", SEATS_PER_FUNCTION,
                "byDate", byDateList
        );
    }

    public List<TheaterEvent> listBySpacecraft(Long spacecraftId) {
        return theaterEventRepository.findBySpacecraftId(spacecraftId);
    }

    public TheaterEvent getById(Long id) {
        return theaterEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater event with ID " + id + " not found"));
    }

    // Mapa de los 100 asientos (libres vs. ocupados) para una funcion puntual de un evento
    public Map<String, Object> getAvailability(Long eventId, LocalDate date) {
        TheaterEvent event = getById(eventId);

        if (date == null || date.isBefore(event.getStartDate()) || date.isAfter(event.getEndDate())) {
            throw new IllegalArgumentException("date must be between " + event.getStartDate() + " and " + event.getEndDate());
        }

        Set<Integer> occupied = new HashSet<>();
        List<TheaterTicket> tickets = theaterTicketRepository.findByEventIdAndFunctionDateAndStatus(eventId, date, TicketStatus.ACTIVE);
        for (TheaterTicket t : tickets) {
            occupied.addAll(t.getSeats());
        }

        List<Integer> available = new ArrayList<>();
        for (int seat = 1; seat <= SEATS_PER_FUNCTION; seat++) {
            if (!occupied.contains(seat)) {
                available.add(seat);
            }
        }

        return Map.of(
                "totalSeats", SEATS_PER_FUNCTION,
                "occupiedSeats", occupied,
                "availableSeats", available
        );
    }
}
'@

# ---------- service/RepairService.java (nuevo) ----------
Write-CodeFile "service\RepairService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.ClosedTheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.DamageCategory;
import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.RepairDamage;
import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.MuseumScheduleRepository;
import com.rocha.spacecraftmanagementsystem.repository.MuseumTicketRepository;
import com.rocha.spacecraftmanagementsystem.repository.RepairRecordRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterEventRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Fase 3: taller de reparacion. Gestion interna de flota (sin venta de entradas ni cara publica).
@Service
public class RepairService {

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private RepairRecordRepository repairRecordRepository;

    @Autowired
    private MuseumScheduleRepository museumScheduleRepository;

    @Autowired
    private MuseumTicketRepository museumTicketRepository;

    @Autowired
    private TheaterEventRepository theaterEventRepository;

    @Autowired
    private TheaterTicketRepository theaterTicketRepository;

    // Catalogo de danos (categoria -> etiqueta + subtipos), para el cascada de selects del frontend.
    // No depende de ninguna nave en particular.
    public Map<String, Object> getDamageCatalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        for (DamageCategory category : DamageCategory.values()) {
            catalog.put(category.name(), Map.of(
                    "label", category.getLabel(),
                    "subtypes", category.getSubtypes()
            ));
        }
        return catalog;
    }

    // Cuenta cuantas entradas activas se cancelarian si esta nave entra al taller ahora.
    // Lo usa el frontend para decidir si mostrar el popup de confirmacion antes de enviar.
    public Map<String, Object> getImpact(Long spacecraftId) {
        getSpacecraftOrThrow(spacecraftId);

        int museumCount = museumTicketRepository.findBySpacecraftIdAndStatus(spacecraftId, TicketStatus.ACTIVE).size();
        List<TheaterEvent> events = theaterEventRepository.findBySpacecraftId(spacecraftId);

        int theaterCount = 0;
        for (TheaterEvent event : events) {
            theaterCount += theaterTicketRepository.findByEventIdAndStatus(event.getId(), TicketStatus.ACTIVE).size();
        }

        int museumSchedulesToClose = museumScheduleRepository.findBySpacecraftIdOrderByDateAsc(spacecraftId).size();

        return Map.of(
                "spacecraftId", spacecraftId,
                "museumTicketsToCancel", museumCount,
                "theaterTicketsToCancel", theaterCount,
                "totalTicketsToCancel", museumCount + theaterCount,
                "museumSchedulesToClose", museumSchedulesToClose,
                "theaterEventsToClose", events.size()
        );
    }

    // Envia la nave al taller: valida que este operativa, cancela entradas activas y cierra
    // (borra) horarios de museo y funciones de teatro, y crea el historial de reparacion con
    // los danos elegidos. request llega del frontend con al menos "damages"; el resto de sus
    // campos (id, status, sentAt) se sobreescriben aqui, igual que en las compras de Fase 2.
    public RepairRecord sendToTaller(Long spacecraftId, RepairRecord request) {
        Spacecraft spacecraft = getSpacecraftOrThrow(spacecraftId);

        if (spacecraft.getStatus() != null && spacecraft.getStatus() != SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException(
                    "Spacecraft " + spacecraftId + " is already en reparacion (" + spacecraft.getStatus() + ")");
        }

        List<RepairDamage> damages = request == null ? null : request.getDamages();
        if (damages == null || damages.isEmpty()) {
            throw new IllegalArgumentException("At least one damage must be selected");
        }
        for (RepairDamage damage : damages) {
            validateDamage(damage);
        }

        int cancelledCount = 0;

        List<MuseumTicket> activeMuseumTickets =
                museumTicketRepository.findBySpacecraftIdAndStatus(spacecraftId, TicketStatus.ACTIVE);
        for (MuseumTicket ticket : activeMuseumTickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
        }
        museumTicketRepository.saveAll(activeMuseumTickets);
        cancelledCount += activeMuseumTickets.size();

        // Snapshot de lo que se va a cerrar - se guarda en el RepairRecord ANTES de borrar,
        // para no perder el historial de a cuanto se disrupta cada nave por reparaciones.
        List<MuseumSchedule> schedules = museumScheduleRepository.findBySpacecraftIdOrderByDateAsc(spacecraftId);
        List<LocalDate> closedMuseumDates = new ArrayList<>();
        for (MuseumSchedule schedule : schedules) {
            closedMuseumDates.add(schedule.getDate());
        }
        museumScheduleRepository.deleteAll(schedules);

        List<TheaterEvent> events = theaterEventRepository.findBySpacecraftId(spacecraftId);
        List<ClosedTheaterEvent> closedTheaterEvents = new ArrayList<>();
        for (TheaterEvent event : events) {
            closedTheaterEvents.add(new ClosedTheaterEvent(
                    event.getEventType(), event.getStartDate(), event.getEndDate(), event.getTime()));

            List<TheaterTicket> activeTickets = theaterTicketRepository.findByEventIdAndStatus(event.getId(), TicketStatus.ACTIVE);
            for (TheaterTicket ticket : activeTickets) {
                ticket.setStatus(TicketStatus.CANCELLED);
            }
            theaterTicketRepository.saveAll(activeTickets);
            cancelledCount += activeTickets.size();
        }
        theaterEventRepository.deleteAll(events);

        request.setId(null);
        request.setSpacecraftId(spacecraftId);
        request.setStatus(SpacecraftStatus.ENTRO_A_TALLER);
        request.setSentAt(LocalDateTime.now());
        request.setFinishedAt(null);
        request.setCancelledTicketCount(cancelledCount);
        request.setClosedMuseumDates(closedMuseumDates);
        request.setClosedTheaterEvents(closedTheaterEvents);
        RepairRecord saved = repairRecordRepository.save(request);

        spacecraft.setStatus(SpacecraftStatus.ENTRO_A_TALLER);
        spacecraftRepository.save(spacecraft);

        return saved;
    }

    // Cambia el sub-estado dentro del taller (ENTRO_A_TALLER, EN_REVISION, ESPERA_REPUESTOS,
    // EN_PROCESO). No se usa para volver a OPERATIVA - eso es finish().
    public RepairRecord advanceStatus(Long spacecraftId, SpacecraftStatus newStatus) {
        Spacecraft spacecraft = getSpacecraftOrThrow(spacecraftId);

        if (spacecraft.getStatus() == null || spacecraft.getStatus() == SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException("Spacecraft " + spacecraftId + " is not currently en el taller");
        }

        if (newStatus == null || newStatus == SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException(
                    "newStatus must be one of ENTRO_A_TALLER, EN_REVISION, ESPERA_REPUESTOS, EN_PROCESO");
        }

        RepairRecord current = getCurrentRecordOrThrow(spacecraftId);
        current.setStatus(newStatus);
        repairRecordRepository.save(current);

        spacecraft.setStatus(newStatus);
        spacecraftRepository.save(spacecraft);

        return current;
    }

    // Finaliza la reparacion: la nave vuelve a OPERATIVA sin importar en que sub-estado estaba.
    public RepairRecord finish(Long spacecraftId) {
        Spacecraft spacecraft = getSpacecraftOrThrow(spacecraftId);

        if (spacecraft.getStatus() == null || spacecraft.getStatus() == SpacecraftStatus.OPERATIVA) {
            throw new IllegalArgumentException("Spacecraft " + spacecraftId + " is not currently en el taller");
        }

        RepairRecord current = getCurrentRecordOrThrow(spacecraftId);
        current.setStatus(SpacecraftStatus.OPERATIVA);
        current.setFinishedAt(LocalDateTime.now());
        repairRecordRepository.save(current);

        spacecraft.setStatus(SpacecraftStatus.OPERATIVA);
        spacecraftRepository.save(spacecraft);

        return current;
    }

    public List<RepairRecord> history(Long spacecraftId) {
        getSpacecraftOrThrow(spacecraftId);
        return repairRecordRepository.findBySpacecraftIdOrderBySentAtDesc(spacecraftId);
    }

    private void validateDamage(RepairDamage damage) {
        if (damage.getCategory() == null) {
            throw new IllegalArgumentException("Each damage must have a category");
        }
        if (damage.getSubtype() == null || damage.getSubtype().isBlank()) {
            throw new IllegalArgumentException("Each damage must have a subtype");
        }
        if (!damage.getCategory().getSubtypes().contains(damage.getSubtype())) {
            throw new IllegalArgumentException(
                    "\"" + damage.getSubtype() + "\" is not a valid subtype for category " + damage.getCategory());
        }
    }

    private Spacecraft getSpacecraftOrThrow(Long spacecraftId) {
        return spacecraftRepository.findById(spacecraftId)
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + spacecraftId + " not found"));
    }

    private RepairRecord getCurrentRecordOrThrow(Long spacecraftId) {
        return repairRecordRepository.findBySpacecraftIdAndFinishedAtIsNull(spacecraftId)
                .orElseThrow(() -> new ResourceNotFoundException("No open repair record found for spacecraft " + spacecraftId));
    }
}
'@

# ---------- controller/RepairController.java (nuevo) ----------
Write-CodeFile "controller\RepairController.java" @'
package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RepairController {

    @Autowired
    private RepairService repairService;

    // Catalogo de categorias/subtipos de dano para el cascada de selects (no depende de la nave)
    @GetMapping("/api/repairs/damage-catalog")
    public ResponseEntity<?> getDamageCatalog() {
        return ResponseEntity.ok(repairService.getDamageCatalog());
    }

    // Cuantas entradas activas se cancelarian si esta nave entra al taller ahora (para el popup)
    @GetMapping("/api/spacecrafts/{id}/repairs/impact")
    public ResponseEntity<?> getImpact(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.getImpact(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Enviar la nave al taller (body: { "damages": [{ "category": "...", "subtype": "..." }] })
    @PostMapping("/api/spacecrafts/{id}/repairs")
    public ResponseEntity<?> sendToTaller(@PathVariable Long id, @RequestBody RepairRecord request) {
        try {
            RepairRecord saved = repairService.sendToTaller(id, request);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Historial de reparaciones de una nave (incluye la abierta, si esta en el taller)
    @GetMapping("/api/spacecrafts/{id}/repairs")
    public ResponseEntity<?> history(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.history(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Cambia el sub-estado dentro del taller (no sirve para volver a OPERATIVA)
    @PatchMapping("/api/spacecrafts/{id}/repairs/current/status")
    public ResponseEntity<?> advanceStatus(@PathVariable Long id, @RequestParam SpacecraftStatus newStatus) {
        try {
            return ResponseEntity.ok(repairService.advanceStatus(id, newStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Finaliza la reparacion: la nave vuelve a OPERATIVA
    @PostMapping("/api/spacecrafts/{id}/repairs/current/finish")
    public ResponseEntity<?> finish(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.finish(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
'@

# ---------- config/CorsConfig.java (se sobreescribe: se agrega el origen del nuevo frontend de taller) ----------
Write-CodeFile "config\CorsConfig.java" @'
package com.rocha.spacecraftmanagementsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://localhost:5174",
                                "http://localhost:5175",
                                "https://spacecraft-system.web.app",
                                "https://spacecraft-system.firebaseapp.com",
                                "https://spacecraft-tickets.web.app",
                                "https://spacecraft-tickets.firebaseapp.com",
                                "https://spacecraft-taller.web.app",
                                "https://spacecraft-taller.firebaseapp.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
'@

Write-Host ""
Write-Host "Listo. 13 archivos escritos (8 nuevos + Spacecraft.java, MuseumTicketRepository.java, MuseumScheduleService.java, TheaterEventService.java y CorsConfig.java actualizados)." -ForegroundColor Green
Write-Host "Siguiente paso: desde la raiz del repo, corre 'mvnw.cmd clean spring-boot:run' para confirmar que arranca sin errores." -ForegroundColor Yellow
