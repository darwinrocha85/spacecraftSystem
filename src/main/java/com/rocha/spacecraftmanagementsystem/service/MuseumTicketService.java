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

    // Fase de integracion: nota que identifica el origen del cargo en el panel de BankIn.
    private static final String BANKIN_NOTE = "naveSpace Tickets";

    @Autowired
    private MuseumTicketRepository museumTicketRepository;

    @Autowired
    private MuseumScheduleRepository museumScheduleRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private BankInPaymentService bankInPaymentService;

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

        if (isBlank(request.getCardId())) {
            throw new IllegalArgumentException("cardId is required to pay with BankIn");
        }

        validateSlot(spacecraft, request.getVisitDate(), request.getVisitTime(), request.getQuantity());

        // Fase de integracion: se cobra ANTES de guardar la entrada. Si BankIn no confirma el
        // cobro (201), no se persiste ningun ticket - el cupo nunca llega a reservarse ni a
        // descontarse de la disponibilidad de ese horario.
        double unitPrice = spacecraft.getTicketPrice() != null ? spacecraft.getTicketPrice() : BankInPaymentService.DEFAULT_TICKET_PRICE;
        double amount = unitPrice * request.getQuantity();
        BankInPaymentService.ChargeResult charge = bankInPaymentService.charge(request.getCardId(), amount, BANKIN_NOTE);

        request.setId(null);
        request.setConfirmationCode(generateConfirmationCode("MUS"));
        request.setStatus(TicketStatus.ACTIVE);
        request.setBankinTransactionId(charge.transactionId());
        request.setAmountCharged(charge.amount());

        MuseumTicket saved = museumTicketRepository.save(request);
        emailNotificationService.sendMuseumConfirmation(saved, spacecraft);
        return saved;
    }

    public MuseumTicket cancel(Long id) {
        MuseumTicket ticket = getActiveOrThrow(id);
        ticket.setStatus(TicketStatus.CANCELLED);
        MuseumTicket saved = museumTicketRepository.save(ticket);
        // Fase de integracion: revierte el cobro en BankIn (best-effort, no bloquea la cancelacion).
        bankInPaymentService.reverse(saved.getBankinTransactionId());
        return saved;
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
