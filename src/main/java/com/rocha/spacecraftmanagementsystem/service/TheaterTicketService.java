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