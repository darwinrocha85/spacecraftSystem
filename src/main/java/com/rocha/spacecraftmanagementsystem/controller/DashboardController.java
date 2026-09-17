package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Fase 6: dashboard de solo lectura para el admin (general y por nave).
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // KPIs generales de toda la flota: ingresos, tickets, ocupacion de hoy, estado de flota y
    // top naves por ingresos.
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return dashboardService.getOverview();
    }

    // KPIs de una nave puntual: mismo tipo de datos que /overview pero acotados a esa nave, mas
    // su historial de reparaciones.
    @GetMapping("/spacecrafts/{id}")
    public Map<String, Object> getSpacecraftDashboard(@PathVariable Long id) {
        return dashboardService.getSpacecraftDashboard(id);
    }

    // Detalle de entradas activas (museo + teatro): fecha/hora, comprador, código de confirmación
    // y desglose de costo. Sin spacecraftId trae toda la flota; con spacecraftId, solo esa nave.
    @GetMapping("/tickets")
    public List<Map<String, Object>> getTicketDetails(@RequestParam(required = false) Long spacecraftId) {
        return dashboardService.getTicketDetails(spacecraftId);
    }
}
