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