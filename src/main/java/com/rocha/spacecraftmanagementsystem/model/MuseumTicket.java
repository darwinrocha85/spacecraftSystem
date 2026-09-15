package com.rocha.spacecraftmanagementsystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

// Fase 2: entrada de museo. Una fila = una compra (quantity = cupos que cubre esa compra).
@Entity
@Table(name = "museum_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MuseumTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spacecraft_id", nullable = false)
    private Long spacecraftId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_time", nullable = false)
    private LocalTime visitTime;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false, length = 150)
    private String buyerEmail;

    @Column(name = "confirmation_code", nullable = false, unique = true, length = 20)
    private String confirmationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;

    // Fase de integracion BankIn: numero de tarjeta ingresado en el checkout. Se usa solo para
    // llamar a BankIn al comprar - nunca se persiste (@Transient) ni se devuelve en la respuesta
    // (WRITE_ONLY), para no guardar el numero de tarjeta en nuestra propia base de datos.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cardId;

    // Referencia al cobro real en BankIn (para poder correlacionar o anular manualmente si hace falta).
    @Column(name = "bankin_transaction_id")
    private Long bankinTransactionId;

    // Monto efectivamente cobrado por BankIn (se guarda aparte del precio de la nave,
    // que puede cambiar despues de la compra).
    @Column(name = "amount_charged")
    private Double amountCharged;
}
