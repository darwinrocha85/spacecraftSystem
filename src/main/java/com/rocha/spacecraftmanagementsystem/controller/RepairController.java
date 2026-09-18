package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import com.rocha.spacecraftmanagementsystem.service.RepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Fase 1 (extraccion del taller a backend Python): este controller quedo reducido a lo que
// spacecraftSystem sigue orquestando (enviar a taller, ver impacto, recibir la nave de vuelta).
// El catalogo de danos, el historial detallado y el cambio de sub-estado ahora viven en
// spacecraft-taller-backend (Python) - ver GET /api/catalog/damages, GET /api/repairs/...,
// PATCH /api/repairs/{id}/status, POST /api/repairs/{id}/budgets, etc. en ese servicio.
@RestController
public class RepairController {

    @Autowired
    private RepairService repairService;

    // Cuantas entradas activas se cancelarian si esta nave entra al taller ahora (para el popup)
    @GetMapping("/api/spacecrafts/{id}/repairs/impact")
    public ResponseEntity<?> getImpact(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.getImpact(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Enviar la nave al taller (body: { "damages": [{ "category": "...", "subtype": "..." }] }).
    // Cancela entradas activas (revirtiendo su cobro) y avisa a spacecraft-taller-backend.
    @PostMapping("/api/spacecrafts/{id}/repairs")
    public ResponseEntity<?> sendToTaller(@PathVariable Long id, @RequestBody RepairRecord request) {
        try {
            RepairRecord saved = repairService.sendToTaller(id, request);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // El dueño de la flota confirma que retiro la nave del taller: vuelve a OPERATIVA en
    // spacecraftSystem. Idempotente. El "ENTREGADA" del lado del taller se confirma aparte, en
    // spacecraft-taller-backend (POST /api/repairs/{id}/receive).
    @PostMapping("/api/spacecrafts/{id}/repairs/current/receive")
    public ResponseEntity<?> receive(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repairService.receiveFromTaller(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
