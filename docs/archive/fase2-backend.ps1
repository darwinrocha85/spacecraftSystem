# Fase 2 - venta de entradas: escribe/actualiza los archivos Java del backend.
# Corre esto desde cualquier lado (usa rutas absolutas). Requiere que el repo
# spacecraftSystem ya exista en la ruta de abajo.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File "C:\ruta\a\fase2-backend.ps1"

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

Write-Host "Escribiendo archivos de Fase 2 en $base ..." -ForegroundColor Cyan

# ---------- model/TicketStatus.java (nuevo) ----------
Write-CodeFile "model\TicketStatus.java" @'
package com.rocha.spacecraftmanagementsystem.model;

public enum TicketStatus {
    ACTIVE,
    CANCELLED
}
'@

# ---------- model/TheaterEventType.java (nuevo) ----------
Write-CodeFile "model\TheaterEventType.java" @'
package com.rocha.spacecraftmanagementsystem.model;

public enum TheaterEventType {
    MUSICA,
    ARTES,
    LIBRE
}
'@

# ---------- model/MuseumTicket.java (nuevo) ----------
Write-CodeFile "model\MuseumTicket.java" @'
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

// Fase 2: entrada de museo. Una fila = una compra (quantity = cupos que cubre esa compra).
@Entity
@Table(name = "museum_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MuseumTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spacecraft_id", nullable = false)
    private Long spacecraftId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_time", nullable = false)
    private LocalTime visitTime;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

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
'@

# ---------- model/TheaterEvent.java (nuevo) ----------
Write-CodeFile "model\TheaterEvent.java" @'
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
'@

# ---------- model/TheaterTicket.java (nuevo) ----------
Write-CodeFile "model\TheaterTicket.java" @'
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
'@

# ---------- exception/ResourceNotFoundException.java (nuevo) ----------
Write-CodeFile "exception\ResourceNotFoundException.java" @'
package com.rocha.spacecraftmanagementsystem.exception;

// Excepcion generica de "no encontrado" para los recursos de Fase 2
// (tickets, eventos) que no ameritan su propia clase dedicada como SpacecraftNotFoundException.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
'@

# ---------- exception/GlobalExceptionHandler.java (se sobreescribe: se le agrega el handler de ResourceNotFoundException) ----------
Write-CodeFile "exception\GlobalExceptionHandler.java" @'
package com.rocha.spacecraftmanagementsystem.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Manejo de excepciones para EntityNotFoundException
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "Entity not found");
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Manejo de excepciones generales
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "An unexpected error occurred");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejo de excepciones específicas personalizadas
    @ExceptionHandler(SpacecraftNotFoundException.class)
    public ResponseEntity<Object> handleSpacecraftNotFoundException(SpacecraftNotFoundException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Fase 2: tickets/eventos no encontrados (busqueda por ID o por codigo de confirmacion)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Agrega otros métodos @ExceptionHandler para manejar excepciones específicas según sea necesario
}
'@

# ---------- config/CorsConfig.java (se sobreescribe: se agregan dominios del frontend de entradas + metodo PATCH) ----------
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
                                "https://spacecraft-system.web.app",
                                "https://spacecraft-system.firebaseapp.com",
                                "https://spacecraft-tickets.web.app",
                                "https://spacecraft-tickets.firebaseapp.com"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
'@

# ---------- repository/MuseumTicketRepository.java (nuevo) ----------
Write-CodeFile "repository\MuseumTicketRepository.java" @'
package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
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
}
'@

# ---------- repository/TheaterEventRepository.java (nuevo) ----------
Write-CodeFile "repository\TheaterEventRepository.java" @'
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
'@

# ---------- repository/TheaterTicketRepository.java (nuevo) ----------
Write-CodeFile "repository\TheaterTicketRepository.java" @'
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
'@

# ---------- service/EmailNotificationService.java (nuevo) ----------
Write-CodeFile "service\EmailNotificationService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de confirmacion best-effort: si falla (SMTP no configurado o credenciales de prueba),
// solo se registra en el log y la compra queda igual de confirmada - nunca bloquea el checkout.
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String SUBJECT = "Esto es una prueba - no vincula de forma legal";

    // required = false: si por algun motivo Spring no llega a crear el bean de mail,
    // la app igual arranca y solo se omite el envio (en vez de fallar el startup completo).
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@spacecraft-system.demo}")
    private String fromAddress;

    public void sendMuseumConfirmation(MuseumTicket ticket, Spacecraft spacecraft) {
        String body = "Confirmacion de visita al museo\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : ticket.getSpacecraftId()) + "\n"
                + "Fecha: " + ticket.getVisitDate() + "\n"
                + "Hora: " + ticket.getVisitTime() + "\n"
                + "Cupos: " + ticket.getQuantity() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n\n"
                + "Esta es una demo. Esta entrada no tiene validez legal ni implica ningun cobro real.";
        send(ticket.getBuyerEmail(), body);
    }

    public void sendTheaterConfirmation(TheaterTicket ticket, TheaterEvent event, Spacecraft spacecraft) {
        String body = "Confirmacion de entradas de teatro\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : event.getSpacecraftId()) + "\n"
                + "Funcion: " + ticket.getFunctionDate() + " " + event.getTime() + "\n"
                + "Asientos: " + ticket.getSeats() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n\n"
                + "Esta es una demo. Esta entrada no tiene validez legal ni implica ningun cobro real.";
        send(ticket.getBuyerEmail(), body);
    }

    private void send(String to, String body) {
        if (mailSender == null) {
            logger.warn("JavaMailSender no disponible; se omite el email de confirmacion a {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(SUBJECT);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("No se pudo enviar el email de confirmacion a {}: {}", to, e.getMessage());
        }
    }
}
'@

# ---------- service/MuseumTicketService.java (nuevo) ----------
Write-CodeFile "service\MuseumTicketService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.MuseumScheduleRepository;
import com.rocha.spacecraftmanagementsystem.repository.MuseumTicketRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MuseumTicketService {

    // Misma ventana movil que MuseumScheduleService (Fase 1): hoy inclusive + 7 dias mas.
    private static final int WINDOW_DAYS = 8;
    private static final int MAX_QUANTITY_PER_PURCHASE = 10;

    @Autowired
    private MuseumTicketRepository museumTicketRepository;

    @Autowired
    private MuseumScheduleRepository museumScheduleRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    // Cupos disponibles por cada turno de una hora, dentro del horario abierto de ese dia
    public List<Map<String, Object>> getAvailability(Long spacecraftId, LocalDate date) {
        Spacecraft spacecraft = spacecraftRepository.findById(spacecraftId)
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + spacecraftId + " not found"));

        if (!Boolean.TRUE.equals(spacecraft.getIsMuseum())) {
            throw new IllegalArgumentException("Spacecraft " + spacecraftId + " is not a museum");
        }

        validateWindow(date);

        MuseumSchedule schedule = museumScheduleRepository.findBySpacecraftIdAndDate(spacecraftId, date)
                .orElse(null);

        List<Map<String, Object>> slots = new ArrayList<>();
        if (schedule == null) {
            return slots; // nave cerrada ese dia (sin horario definido): sin turnos disponibles
        }

        int capacity = spacecraft.getMuseumCapacity() == null ? 0 : spacecraft.getMuseumCapacity();
        LocalTime cursor = schedule.getOpenTime();
        while (cursor.isBefore(schedule.getCloseTime())) {
            int booked = museumTicketRepository.sumActiveQuantity(spacecraftId, date, cursor);
            int available = Math.max(0, capacity - booked);
            slots.add(Map.of(
                    "time", cursor.toString(),
                    "capacity", capacity,
                    "booked", booked,
                    "available", available
            ));
            cursor = cursor.plusHours(1);
        }
        return slots;
    }

    public MuseumTicket purchase(MuseumTicket request) {
        Spacecraft spacecraft = spacecraftRepository.findById(request.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + request.getSpacecraftId() + " not found"));

        if (!Boolean.TRUE.equals(spacecraft.getIsMuseum())) {
            throw new IllegalArgumentException("Spacecraft " + spacecraft.getId() + " is not a museum");
        }

        if (request.getQuantity() == null || request.getQuantity() < 1 || request.getQuantity() > MAX_QUANTITY_PER_PURCHASE) {
            throw new IllegalArgumentException("quantity must be between 1 and " + MAX_QUANTITY_PER_PURCHASE);
        }

        if (isBlank(request.getBuyerName()) || isBlank(request.getBuyerEmail())) {
            throw new IllegalArgumentException("buyerName and buyerEmail are required");
        }

        validateSlot(spacecraft, request.getVisitDate(), request.getVisitTime(), request.getQuantity());

        request.setId(null);
        request.setConfirmationCode(generateConfirmationCode("MUS"));
        request.setStatus(TicketStatus.ACTIVE);

        MuseumTicket saved = museumTicketRepository.save(request);
        emailNotificationService.sendMuseumConfirmation(saved, spacecraft);
        return saved;
    }

    public MuseumTicket cancel(Long id) {
        MuseumTicket ticket = getActiveOrThrow(id);
        ticket.setStatus(TicketStatus.CANCELLED);
        return museumTicketRepository.save(ticket);
    }

    public MuseumTicket reschedule(Long id, LocalDate newDate, LocalTime newTime) {
        MuseumTicket ticket = getActiveOrThrow(id);
        Spacecraft spacecraft = spacecraftRepository.findById(ticket.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + ticket.getSpacecraftId() + " not found"));

        boolean sameSlot = newDate != null && newDate.equals(ticket.getVisitDate())
                && newTime != null && newTime.equals(ticket.getVisitTime());

        if (!sameSlot) {
            validateSlot(spacecraft, newDate, newTime, ticket.getQuantity());
            ticket.setVisitDate(newDate);
            ticket.setVisitTime(newTime);
        }
        return museumTicketRepository.save(ticket);
    }

    public MuseumTicket findByConfirmationCode(String code) {
        return museumTicketRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No museum ticket found with confirmation code " + code));
    }

    private MuseumTicket getActiveOrThrow(Long id) {
        MuseumTicket ticket = museumTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Museum ticket with ID " + id + " not found"));
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalArgumentException("Museum ticket " + id + " is not active");
        }
        return ticket;
    }

    private void validateWindow(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(WINDOW_DAYS - 1);
        if (date == null || date.isBefore(today) || date.isAfter(limit)) {
            throw new IllegalArgumentException(
                    "date must be between " + today + " and " + limit + " (today plus " + (WINDOW_DAYS - 1) + " days)");
        }
    }

    private void validateSlot(Spacecraft spacecraft, LocalDate date, LocalTime time, int quantity) {
        validateWindow(date);

        if (time == null || time.getMinute() != 0 || time.getSecond() != 0) {
            throw new IllegalArgumentException("visitTime must be on the hour (e.g. 10:00)");
        }

        MuseumSchedule schedule = museumScheduleRepository.findBySpacecraftIdAndDate(spacecraft.getId(), date)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Spacecraft " + spacecraft.getId() + " has no schedule for " + date + " (closed that day)"));

        if (time.isBefore(schedule.getOpenTime()) || !time.isBefore(schedule.getCloseTime())) {
            throw new IllegalArgumentException(
                    "visitTime must be between " + schedule.getOpenTime() + " and " + schedule.getCloseTime());
        }

        int booked = museumTicketRepository.sumActiveQuantity(spacecraft.getId(), date, time);
        int capacity = spacecraft.getMuseumCapacity() == null ? 0 : spacecraft.getMuseumCapacity();
        if (booked + quantity > capacity) {
            throw new IllegalArgumentException(
                    "Not enough capacity: " + Math.max(0, capacity - booked) + " spot(s) left at " + time + " on " + date);
        }
    }

    private String generateConfirmationCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
'@

# ---------- service/TheaterEventService.java (nuevo) ----------
Write-CodeFile "service\TheaterEventService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
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

# ---------- service/TheaterTicketService.java (nuevo) ----------
Write-CodeFile "service\TheaterTicketService.java" @'
package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.model.TicketStatus;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TheaterTicketService {

    private static final int MAX_SEATS_PER_PURCHASE = 5;

    @Autowired
    private TheaterTicketRepository theaterTicketRepository;

    @Autowired
    private TheaterEventService theaterEventService;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    public TheaterTicket purchase(TheaterTicket request) {
        TheaterEvent event = theaterEventService.getById(request.getEventId());

        if (request.getFunctionDate() == null
                || request.getFunctionDate().isBefore(event.getStartDate())
                || request.getFunctionDate().isAfter(event.getEndDate())) {
            throw new IllegalArgumentException("functionDate must be between " + event.getStartDate() + " and " + event.getEndDate());
        }

        List<Integer> seats = request.getSeats();
        if (seats == null || seats.isEmpty() || seats.size() > MAX_SEATS_PER_PURCHASE) {
            throw new IllegalArgumentException("seats must contain between 1 and " + MAX_SEATS_PER_PURCHASE + " seat numbers");
        }

        Set<Integer> uniqueSeats = new HashSet<>(seats);
        if (uniqueSeats.size() != seats.size()) {
            throw new IllegalArgumentException("seats must not contain duplicates");
        }

        for (Integer seat : seats) {
            if (seat == null || seat < 1 || seat > TheaterEventService.SEATS_PER_FUNCTION) {
                throw new IllegalArgumentException("seat numbers must be between 1 and " + TheaterEventService.SEATS_PER_FUNCTION);
            }
        }

        if (isBlank(request.getBuyerName()) || isBlank(request.getBuyerEmail())) {
            throw new IllegalArgumentException("buyerName and buyerEmail are required");
        }

        List<TheaterTicket> existing = theaterTicketRepository.findByEventIdAndFunctionDateAndStatus(
                request.getEventId(), request.getFunctionDate(), TicketStatus.ACTIVE);
        Set<Integer> occupied = new HashSet<>();
        for (TheaterTicket t : existing) {
            occupied.addAll(t.getSeats());
        }
        for (Integer seat : seats) {
            if (occupied.contains(seat)) {
                throw new IllegalArgumentException("Seat " + seat + " is already taken for that function");
            }
        }

        request.setId(null);
        request.setConfirmationCode("THT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setStatus(TicketStatus.ACTIVE);

        TheaterTicket saved = theaterTicketRepository.save(request);

        Spacecraft spacecraft = spacecraftRepository.findById(event.getSpacecraftId()).orElse(null);
        emailNotificationService.sendTheaterConfirmation(saved, event, spacecraft);

        return saved;
    }

    public TheaterTicket cancel(Long id) {
        TheaterTicket ticket = theaterTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater ticket with ID " + id + " not found"));
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalArgumentException("Theater ticket " + id + " is not active");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        return theaterTicketRepository.save(ticket);
    }

    public TheaterTicket findByConfirmationCode(String code) {
        return theaterTicketRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No theater ticket found with confirmation code " + code));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
'@

# ---------- controller/MuseumTicketController.java (nuevo) ----------
Write-CodeFile "controller\MuseumTicketController.java" @'
package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.service.MuseumTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
public class MuseumTicketController {

    @Autowired
    private MuseumTicketService museumTicketService;

    // Cupos disponibles por hora para una nave museo en una fecha dada
    @GetMapping("/api/museum/{spacecraftId}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable Long spacecraftId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            List<Map<String, Object>> availability = museumTicketService.getAvailability(spacecraftId, date);
            return ResponseEntity.ok(availability);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Compra de entradas de museo (spacecraftId, visitDate, visitTime, quantity <=10, buyerName, buyerEmail)
    @PostMapping("/api/museum-tickets")
    public ResponseEntity<?> purchase(@RequestBody MuseumTicket request) {
        try {
            MuseumTicket saved = museumTicketService.purchase(request);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Buscar una entrada por su codigo de confirmacion ("Mis entradas")
    @GetMapping("/api/museum-tickets/search")
    public ResponseEntity<?> search(@RequestParam String code) {
        return ResponseEntity.ok(museumTicketService.findByConfirmationCode(code));
    }

    // Cancelar una entrada activa (libera el cupo de inmediato)
    @PatchMapping("/api/museum-tickets/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(museumTicketService.cancel(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Reprogramar una entrada activa a otro dia/hora, si hay cupo en el destino
    @PatchMapping("/api/museum-tickets/{id}/reschedule")
    public ResponseEntity<?> reschedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime visitTime
    ) {
        try {
            return ResponseEntity.ok(museumTicketService.reschedule(id, visitDate, visitTime));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
'@

# ---------- controller/TheaterEventController.java (nuevo) ----------
Write-CodeFile "controller\TheaterEventController.java" @'
package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.service.TheaterEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/theater-events")
public class TheaterEventController {

    @Autowired
    private TheaterEventService theaterEventService;

    // Crea un evento: se repite todos los dias del rango a la misma hora; valida que no choque
    // con otro evento de la misma nave en esa franja
    @PostMapping
    public ResponseEntity<?> create(@RequestBody TheaterEvent event) {
        try {
            TheaterEvent saved = theaterEventService.create(event);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Lista los eventos de una nave
    @GetMapping
    public List<TheaterEvent> listBySpacecraft(@RequestParam Long spacecraftId) {
        return theaterEventService.listBySpacecraft(spacecraftId);
    }

    // Detalle de un evento puntual (lo usa el frontend para mostrar nave/hora en "Mis entradas")
    @GetMapping("/{id}")
    public TheaterEvent getById(@PathVariable Long id) {
        return theaterEventService.getById(id);
    }

    // Edita tipo/horario/rango de fechas de un evento existente
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TheaterEvent event) {
        try {
            TheaterEvent updated = theaterEventService.update(id, event);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Borra un evento (rechaza si ya tiene entradas activas vendidas)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            theaterEventService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Resumen de ventas del evento: total de asientos vendidos y desglose por fecha de funcion
    @GetMapping("/{id}/sales")
    public ResponseEntity<?> getSales(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(theaterEventService.getSales(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Mapa de los 100 asientos (libres/ocupados) para una funcion puntual (fecha dentro del rango del evento)
    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            return ResponseEntity.ok(theaterEventService.getAvailability(id, date));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
'@

# ---------- controller/TheaterTicketController.java (nuevo) ----------
Write-CodeFile "controller\TheaterTicketController.java" @'
package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import com.rocha.spacecraftmanagementsystem.service.TheaterTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/theater-tickets")
public class TheaterTicketController {

    @Autowired
    private TheaterTicketService theaterTicketService;

    // Compra de entradas de teatro (eventId, functionDate, seats[<=5], buyerName, buyerEmail)
    @PostMapping
    public ResponseEntity<?> purchase(@RequestBody TheaterTicket request) {
        try {
            TheaterTicket saved = theaterTicketService.purchase(request);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Buscar una entrada por su codigo de confirmacion ("Mis entradas")
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String code) {
        return ResponseEntity.ok(theaterTicketService.findByConfirmationCode(code));
    }

    // Cancelar una entrada activa (libera los asientos)
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(theaterTicketService.cancel(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
'@

Write-Host ""
Write-Host "Listo. 16 archivos escritos (14 nuevos + GlobalExceptionHandler.java y CorsConfig.java actualizados)." -ForegroundColor Green
Write-Host "Siguiente paso: desde la raiz del repo, corre 'mvnw.cmd clean spring-boot:run' para confirmar que arranca sin errores." -ForegroundColor Yellow
