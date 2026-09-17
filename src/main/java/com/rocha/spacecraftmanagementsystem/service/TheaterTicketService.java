package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.ResourceNotFoundException;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
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

    // Fase de integracion: nota que identifica el origen del cargo en el panel de BankIn.
    private static final String BANKIN_NOTE = "naveSpace Tickets";

    @Autowired
    private TheaterTicketRepository theaterTicketRepository;

    @Autowired
    private TheaterEventService theaterEventService;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private BankInPaymentService bankInPaymentService;

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

        if (isBlank(request.getCardId())) {
            throw new IllegalArgumentException("cardId is required to pay with BankIn");
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

        Spacecraft spacecraft = spacecraftRepository.findById(event.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + event.getSpacecraftId() + " not found"));

        // Fase de integracion: se cobra ANTES de guardar la entrada. Si BankIn no confirma el
        // cobro (201), no se persiste ningun ticket - los asientos nunca llegan a ocuparse.
        double unitPrice = spacecraft.getTicketPrice() != null ? spacecraft.getTicketPrice() : BankInPaymentService.DEFAULT_TICKET_PRICE;
        double amount = unitPrice * seats.size();
        BankInPaymentService.ChargeResult charge = bankInPaymentService.charge(request.getCardId(), amount, BANKIN_NOTE);

        request.setId(null);
        request.setConfirmationCode("THT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setStatus(TicketStatus.ACTIVE);
        request.setBankinTransactionId(charge.transactionId());
        request.setAmountCharged(charge.amount());

        TheaterTicket saved = theaterTicketRepository.save(request);

        emailNotificationService.sendTheaterConfirmation(saved, event, spacecraft);

        return saved;
    }

    public TheaterTicket cancel(Long id) {
        TheaterTicket ticket = theaterTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater ticket with ID " + id + " not found"));
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalArgumentException("Theater ticket " + id + " is not active");
        }

        // El TheaterEvent puede haber sido borrado desde la compra (Fase 3: se borra al pasar la nave a
        // reparacion) - se busca con calma, sin romper la cancelacion si ya no existe.
        TheaterEvent event = null;
        Spacecraft spacecraft = null;
        try {
            event = theaterEventService.getById(ticket.getEventId());
            spacecraft = spacecraftRepository.findById(event.getSpacecraftId()).orElse(null);
        } catch (ResourceNotFoundException e) {
            // el evento ya no existe: se cancela igual, el email de cancelacion se manda sin esos datos.
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        TheaterTicket saved = theaterTicketRepository.save(ticket);
        // Fase de integracion: revierte el cobro en BankIn (best-effort, no bloquea la cancelacion).
        boolean refunded = bankInPaymentService.reverse(saved.getBankinTransactionId());
        // Fase 7: email de cancelacion, mismo patron best-effort que el de compra - nunca bloquea la cancelacion.
        emailNotificationService.sendTheaterCancellation(saved, event, spacecraft, refunded);
        return saved;
    }

    public TheaterTicket findByConfirmationCode(String code) {
        return theaterTicketRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No theater ticket found with confirmation code " + code));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
