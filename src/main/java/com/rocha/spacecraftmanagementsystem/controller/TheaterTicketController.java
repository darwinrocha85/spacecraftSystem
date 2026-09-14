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