package com.rocha.spacecraftmanagementsystem.model;

// Fase 3: estado operativo de la nave. OPERATIVA es el default; los demas son sub-estados
// dentro del taller de reparacion (se avanzan manualmente desde el admin).
public enum SpacecraftStatus {
    OPERATIVA,
    ENTRO_A_TALLER,
    EN_REVISION,
    ESPERA_REPUESTOS,
    EN_PROCESO
}