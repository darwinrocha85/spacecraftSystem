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