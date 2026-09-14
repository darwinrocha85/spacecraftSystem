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