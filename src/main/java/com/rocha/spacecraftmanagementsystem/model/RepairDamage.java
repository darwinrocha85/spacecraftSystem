package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fase 3: un dano puntual (categoria + subtipo) dentro de un RepairRecord.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairDamage {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private DamageCategory category;

    @Column(name = "subtype", nullable = false, length = 60)
    private String subtype;
}