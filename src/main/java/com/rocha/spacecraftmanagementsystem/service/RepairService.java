package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.ClosedTheaterEvent;
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
import java.util.List;
import java.util.Map;

// Fase 3: taller de reparacion. Gestion interna de flota (sin venta de entradas ni cara publica).
//
// Fase 1 (extraccion del taller a backend Python): spacecraftSystem sigue siendo quien orquesta
// "enviar a taller" - valida, cancela entradas activas y revierte sus cobros con BankIn (logica
// preexistente) - pero el detalle fino de la reparacion (sub-estados, presupuesto, repuestos) se
// movio a spacecraft-taller-backend (Python). Por eso ya no existen aca advanceStatus()/finish()
// (eran del sub-estado detallado); en su lugar hay receiveFromTaller(), que es lo unico que
// necesita seguir en Java porque toca el Spacecraft.status de este backend.
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

    @Autowired
    private BankInPaymentService bankInPaymentService;

    @Autowired
    private TallerBackendClient tallerBackendClient;

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

    // Envia la nave al taller: valida que este operativa, cancela entradas activas (revirtiendo
    // su cobro en BankIn) y cierra (borra) horarios de museo y funciones de teatro, crea el
    // resumen de historial local, y recien despues avisa al backend de taller (Python) para que
    // cree su propio registro de reparacion con el detalle de danos.
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
        // Fase 1: revierte el cobro de cada entrada de museo cancelada (best-effort, mismo patron
        // que MuseumTicketService.cancel() - bug encontrado al extraer el taller: antes esta
        // cancelacion masiva no revertia ningun cobro).
        for (MuseumTicket ticket : activeMuseumTickets) {
            bankInPaymentService.reverse(ticket.getBankinTransactionId());
        }

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
            // Fase 1: idem museo - revierte el cobro de cada entrada de teatro cancelada.
            for (TheaterTicket ticket : activeTickets) {
                bankInPaymentService.reverse(ticket.getBankinTransactionId());
            }
        }
        theaterEventRepository.deleteAll(events);

        request.setId(null);
        request.setSpacecraftId(spacecraftId);
        request.setStatus(SpacecraftStatus.EN_TALLER);
        request.setSentAt(LocalDateTime.now());
        request.setFinishedAt(null);
        request.setCancelledTicketCount(cancelledCount);
        request.setClosedMuseumDates(closedMuseumDates);
        request.setClosedTheaterEvents(closedTheaterEvents);
        RepairRecord saved = repairRecordRepository.save(request);

        spacecraft.setStatus(SpacecraftStatus.EN_TALLER);
        spacecraftRepository.save(spacecraft);

        // Fase 1: recien ahora que la cancelacion ya esta confirmada se avisa al backend de
        // taller (Python). Best-effort: si falla, no se revierte nada de lo anterior (ver
        // TallerBackendClient) - solo se refleja en la respuesta para que el admin lo note.
        boolean synced = tallerBackendClient.notifyRepairCreated(
                spacecraftId, spacecraft.getName(), spacecraft.getSpacecraftType(),
                damages, closedMuseumDates, closedTheaterEvents);
        saved.setTallerSyncFailed(!synced);

        return saved;
    }

    // Fase 1: el dueño de la flota confirma que retiro la nave del taller (equivalente, del lado
    // Java, al POST /repairs/{id}/receive de spacecraft-taller-backend). Idempotente: si la nave
    // ya esta OPERATIVA, no hace nada y devuelve el ultimo registro tal cual.
    public RepairRecord receiveFromTaller(Long spacecraftId) {
        Spacecraft spacecraft = getSpacecraftOrThrow(spacecraftId);

        if (spacecraft.getStatus() == null || spacecraft.getStatus() == SpacecraftStatus.OPERATIVA) {
            return repairRecordRepository.findBySpacecraftIdOrderBySentAtDesc(spacecraftId).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Spacecraft " + spacecraftId + " no tiene historial de reparaciones"));
        }

        RepairRecord current = getCurrentRecordOrThrow(spacecraftId);
        current.setStatus(SpacecraftStatus.OPERATIVA);
        current.setFinishedAt(LocalDateTime.now());
        repairRecordRepository.save(current);

        spacecraft.setStatus(SpacecraftStatus.OPERATIVA);
        spacecraftRepository.save(spacecraft);

        return current;
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
