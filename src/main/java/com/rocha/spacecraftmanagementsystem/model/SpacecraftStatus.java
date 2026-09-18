package com.rocha.spacecraftmanagementsystem.model;

// Fase 1 (extraccion del taller a backend Python): estado operativo de la nave, simplificado a
// solo 2 valores reales. El detalle fino del taller (RECIBIDA, EN_REVISION, EN_TRABAJO,
// ESPERANDO_APROBACION_PRESUPUESTO, LISTA_PARA_SALIR, ENTREGADA) ahora vive en
// spacecraft-taller-backend (Python) - spacecraftSystem solo necesita saber si la nave esta
// operativa o en el taller para el resto del sistema (venta de entradas, marketing, dashboard).
public enum SpacecraftStatus {
    OPERATIVA,
    EN_TALLER
}
