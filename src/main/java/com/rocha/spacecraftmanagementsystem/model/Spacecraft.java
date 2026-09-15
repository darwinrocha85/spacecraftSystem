package com.rocha.spacecraftmanagementsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "name", nullable = false, length = 100) // Nombre de la columna, longitud maxima
    private String name;

    @Column(name = "franchise", nullable = false, length = 50) // Serie o pelicula a la que pertenece
    private String franchise;

    @Column(name = "crew_capacity") // Capacidad de tripulacion
    private Integer crewCapacity;

    @Column(name = "speed") // Velocidad maxima en alguna unidad
    private Double speed;

    @Column(name = "spacecraft_type", length = 50) // Tipo de nave
    private String spacecraftType;

    @Column(name = "is_armed") // Indica si esta equipada con armas
    private Boolean isArmed;

    @Column(name = "is_museum") // Indica si la nave funciona como museo
    private Boolean isMuseum;

    @Column(name = "is_theater") // Indica si la nave funciona como teatro
    private Boolean isTheater;

    @Column(name = "museum_capacity") // Maximo de personas dentro del museo al mismo tiempo (obligatorio si isMuseum = true)
    private Integer museumCapacity;

    // Fase de integracion BankIn: precio de la entrada (aplica tanto a museo como a teatro).
    // Nullable a proposito: si no esta configurado, el cobro usa un precio por defecto
    // (ver BankInPaymentService.DEFAULT_TICKET_PRICE) en vez de fallar la compra.
    @Column(name = "ticket_price")
    private Double ticketPrice;

    // Fase 3: estado operativo / taller de reparacion. Default OPERATIVA para naves nuevas.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private SpacecraftStatus status = SpacecraftStatus.OPERATIVA;
}
