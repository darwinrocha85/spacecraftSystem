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
                + "Esta es una demo educativa. El cobro fue procesado de verdad por BankIn (entorno demo), "
                + "pero esta entrada no tiene validez legal.";
        send(ticket.getBuyerEmail(), body);
    }

    public void sendTheaterConfirmation(TheaterTicket ticket, TheaterEvent event, Spacecraft spacecraft) {
        String body = "Confirmacion de entradas de teatro\n\n"
                + "Nave: " + (spacecraft != null ? spacecraft.getName() : event.getSpacecraftId()) + "\n"
                + "Funcion: " + ticket.getFunctionDate() + " " + event.getTime() + "\n"
                + "Asientos: " + ticket.getSeats() + "\n"
                + "Codigo de confirmacion: " + ticket.getConfirmationCode() + "\n"
                + "Cobro BankIn: " + formatAmount(ticket.getAmountCharged()) + " (transaccion #" + ticket.getBankinTransactionId() + ")\n\n"
                + "Esta es una demo educativa. El cobro fue procesado de verdad por BankIn (entorno demo), "
                + "pero esta entrada no tiene validez legal.";
        send(ticket.getBuyerEmail(), body);
    }

    private String formatAmount(Double amount) {
        return amount == null ? "-" : String.format("$%.2f", amount);
    }

    private void send(String to, String body) {
        if (mailSender == null) {
            logger.warn("JavaMailSender no disponible; se omite el email de confirmacion a {}", to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(SUBJECT);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("No se pudo enviar el email de confirmacion a {}: {}", to, e.getMessage());
        }
    }
}
