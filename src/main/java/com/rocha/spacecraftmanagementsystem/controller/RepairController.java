package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RepairController {

    @Autowired
    private RepairService repairService;

    // Catalogo de categorias/subtipos de dano para el cascada de selects (no depende de la nave)
    @GetMapping("/api/repairs/damage-catalog")
    public ResponseEntity<?> getDamageCatalog() {
        return ResponseEntity.ok(repairService.getDamageCatalog());
    }

    // Cuantas entradas activas se cancelarian si esta nave entra al taller ahora (para el popup)
    @GetMapping("/api/spacecrafts/{id}/repairs/impact")
    public ResponseEntity<?> getImpact(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.getImpact(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Enviar la nave al taller (body: { "damages": [{ "category": "...", "subtype": "..." }] })
    @PostMapping("/api/spacecrafts/{id}/repairs")
    public ResponseEntity<?> sendToTaller(@PathVariable Long id, @RequestBody RepairRecord request) {
        try {
            RepairRecord saved = repairService.sendToTaller(id, request);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Historial de reparaciones de una nave (incluye la abierta, si esta en el taller)
    @GetMapping("/api/spacecrafts/{id}/repairs")
    public ResponseEntity<?> history(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.history(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Cambia el sub-estado dentro del taller (no sirve para volver a OPERATIVA)
    @PatchMapping("/api/spacecrafts/{id}/repairs/current/status")
    public ResponseEntity<?> advanceStatus(@PathVariable Long id, @RequestParam SpacecraftStatus newStatus) {
        try {
            return ResponseEntity.ok(repairService.advanceStatus(id, newStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Finaliza la reparacion: la nave vuelve a OPERATIVA
    @PostMapping("/api/spacecrafts/{id}/repairs/current/finish")
    public ResponseEntity<?> finish(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.finish(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}