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