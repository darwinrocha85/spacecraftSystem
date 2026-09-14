package com.rocha.spacecraftmanagementsystem.model;

import java.util.List;

// Fase 3: taxonomia de danos para el taller (cascada categoria -> subtipo en el frontend).
// Fuente unica de verdad: el frontend la consume via GET /api/repairs/damage-catalog.
public enum DamageCategory {

    CASCO_ESTRUCTURA("Casco y Estructura", List.of(
            "Abolladura", "Grieta", "Perforacion", "Dano termico")),
    PROPULSION("Propulsion", List.of(
            "Motor principal", "Propulsores RCS", "Sistema de combustible", "Sobrecalentamiento")),
    ELECTRICO_AVIONICA("Electrico y Avionica", List.of(
            "Cableado", "Computadora de vuelo", "Sensores", "Falla de energia/baterias")),
    SOPORTE_VITAL("Soporte Vital", List.of(
            "Oxigeno", "Presurizacion", "Control de temperatura")),
    COMUNICACIONES("Comunicaciones", List.of(
            "Antena/transmisor", "Sistema de navegacion"));

    private final String label;
    private final List<String> subtypes;

    DamageCategory(String label, List<String> subtypes) {
        this.label = label;
        this.subtypes = subtypes;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getSubtypes() {
        return subtypes;
    }
}