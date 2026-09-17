package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.model.MuseumTicket;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.TheaterTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envio de confirmacion best-effort: si falla (SMTP no configurado o credenciales de prueba),
// solo se registra en el log y la compra queda igual de confirmada - nunca bloquea el checkout.
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String SUBJECT = "Esto es una prueba - no vincula de forma legal";
    // Fase 7: asunto propio para el email de cancelacion, para distinguirlo del de compra en la bandeja.
    private static final String CANCELLATION_SUBJECT = "Cancelacion confirmada - Esto es una prueba - no vincula de forma legal";

    // required = false: si por algun motivo Spring no llega a crear el bean de mail,
    // la app igual arranca y solo se omite el envio (en vez de fallar el startup completo).
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@spacecraft-system.demo}")
    private String fromAddress;

    public void sendMuseumConfirmation(MuseumTicket ticket, Spacecraft spacecraft) {
        String body = "Confirmacion de visita al museo\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : ticket.getSpacecraftId()) + "\n"
                + "Fecha: " + ticket.getVisitDate() + "\n"
                + "Hora: " + ticket.getVisitTime() + "\n"
                + "Cupos: " + ticket.getQuantity() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n"
                + "Cobro BankIn: " + formatAmount(ticket.getAmountCharged()) + " (transaccion #" + ticket.getBankinTransactionId() + ")\n\n"
                + "Esta es una demo educativa. El cobro fue procesado de verdad por BankIn (entorno demo, no es un cobro real), "
                + "pero esta entrada no tiene validez legal.";
        send(ticket.getBuyerEmail(), SUBJECT, body);
    }

    public void sendTheaterConfirmation(TheaterTicket ticket, TheaterEvent event, Spacecraft spacecraft) {
        String body = "Confirmacion de entradas de teatro\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : event.getSpacecraftId()) + "\n"
                + "Funcion: " + ticket.getFunctionDate() + " " + event.getTime() + "\n"
                + "Asientos: " + ticket.getSeats() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n"
                + "Cobro BankIn: " + formatAmount(ticket.getAmountCharged()) + " (transaccion #" + ticket.getBankinTransactionId() + ")\n\n"
                + "Esta es una demo educativa. El cobro fue procesado de verdad por BankIn (entorno demo, no es un cobro real), "
                + "pero esta entrada no tiene validez legal.";
        send(ticket.getBuyerEmail(), SUBJECT, body);
    }

    // Fase 7: email de cancelacion. `refunded` viene de BankInPaymentService.reverse() (best-effort,
    // ver TicketService.cancel()) para poder decirle al comprador con precision si el reembolso quedo
    // confirmado en BankIn o si todavia no se pudo confirmar (sin prometer un reintento automatico).
    public void sendMuseumCancellation(MuseumTicket ticket, Spacecraft spacecraft, boolean refunded) {
        String body = "Cancelacion de visita al museo\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : ticket.getSpacecraftId()) + "\n"
                + "Fecha: " + ticket.getVisitDate() + "\n"
                + "Hora: " + ticket.getVisitTime() + "\n"
                + "Cupos: " + ticket.getQuantity() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n"
                + refundLine(refunded, ticket.getAmountCharged(), ticket.getBankinTransactionId()) + "\n\n"
                + "Esta es una demo educativa. La entrada fue cancelada; el reembolso se procesa a traves de "
                + "BankIn (entorno demo, no es un cobro real), pero esta cancelacion no tiene validez legal.";
        send(ticket.getBuyerEmail(), CANCELLATION_SUBJECT, body);
    }

    public void sendTheaterCancellation(TheaterTicket ticket, TheaterEvent event, Spacecraft spacecraft, boolean refunded) {
        // event puede llegar null: si la nave paso a reparacion, el TheaterEvent se borra (Fase 3) y
        // TheaterTicketService.cancel() sigue adelante igual - el email se manda con lo que haya.
        String naveLabel = spacecraft != null
                ? spacecraft.getName()
                : (event != null ? "Nave #" + event.getSpacecraftId() : "Nave no disponible");
        String funcion = event != null ? ticket.getFunctionDate() + " " + event.getTime() : String.valueOf(ticket.getFunctionDate());
        String body = "Cancelacion de entradas de teatro\n\n"
                + "Nave: " + naveLabel + "\n"
                + "Funcion: " + funcion + "\n"
                + "Asientos: " + ticket.getSeats() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n"
                + refundLine(refunded, ticket.getAmountCharged(), ticket.getBankinTransactionId()) + "\n\n"
                + "Esta es una demo educativa. La entrada fue cancelada; el reembolso se procesa a traves de "
                + "BankIn (entorno demo, no es un cobro real), pero esta cancelacion no tiene validez legal.";
        send(ticket.getBuyerEmail(), CANCELLATION_SUBJECT, body);
    }

    private String refundLine(boolean refunded, Double amount, Long transactionId) {
        if (transactionId == null) {
            return "Reembolso BankIn: no aplica (esta entrada no tenia un cobro registrado)";
        }
        String amountStr = formatAmount(amount);
        return refunded
                ? "Reembolso BankIn: " + amountStr + " revertido correctamente (transaccion #" + transactionId + ")"
                : "Reembolso BankIn: " + amountStr + " - no se pudo confirmar la reversion automatica en este momento "
                        + "(transaccion #" + transactionId + "); si no ves el reembolso, contacta soporte.";
    }

    private String formatAmount(Double amount) {
        return amount == null ? "-" : String.format("$%.2f", amount);
    }

    private void send(String to, String subject, String body) {
        if (mailSender == null) {
            logger.warn("JavaMailSender no disponible; se omite el email a {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("No se pudo enviar el email a {}: {}", to, e.getMessage());
        }
    }
}
