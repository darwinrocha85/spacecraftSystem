package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "spacecrafts") // Nombre de la tabla en la base de datos
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Spacecraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100) // Nombre de la columna, longitud máxima
    private String name;

    @Column(name = "franchise", nullable = false, length = 50) // Serie o película a la que pertenece
    private String franchise;

    @Column(name = "crew_capacity") // Capacidad de tripulación
    private Integer crewCapacity;

    @Column(name = "speed") // Velocidad máxima en alguna unidad
    private Double speed;

    @Column(name = "spacecraft_type", length = 50) // Tipo de nave
    private String spacecraftType;

    @Column(name = "is_armed") // Indica si está equipada con armas
    private Boolean isArmed;
}

