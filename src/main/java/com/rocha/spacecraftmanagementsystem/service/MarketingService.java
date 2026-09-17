package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.model.SpacecraftStatus;
import com.rocha.spacecraftmanagementsystem.model.TheaterEvent;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import com.rocha.spacecraftmanagementsystem.repository.TheaterEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fase 5: API de solo lectura para el landing de marketing (spacecraft-events-landing).
// Compone Spacecraft + TheaterEvent y filtra lo que realmente esta reservable hoy:
// - Museo: nave habilitada como museo (isMuseum) y ademas OPERATIVA (una nave en el
//   taller no deberia ofrecerse como experiencia abierta, aunque SpacecraftService.getVenues()
//   no filtra por status para el panel interno de la tienda de entradas).
// - Teatro: eventos vigentes (endDate >= hoy). En la practica un TheaterEvent siempre
//   pertenece a una nave OPERATIVA porque se borra al entrar a taller (Fase 3), pero se
//   valida igual por seguridad ante datos inconsistentes.
@Service
public class MarketingService {

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    @Autowired
    private TheaterEventRepository theaterEventRepository;

    public Map<String, Object> getExperiences() {
        List<Map<String, Object>> museums = new ArrayList<>();
        for (Spacecraft s : spacecraftRepository.findByIsMuseumTrue()) {
            if (!isOperativa(s)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("name", s.getName());
            item.put("franchise", s.getFranchise());
            item.put("museumCapacity", s.getMuseumCapacity());
            item.put("ticketPrice", ticketPriceOf(s));
            museums.add(item);
        }

        List<Map<String, Object>> theaterEvents = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (TheaterEvent e : theaterEventRepository.findAll()) {
            if (e.getEndDate() == null || e.getEndDate().isBefore(today)) {
                continue; // ya finalizo
            }
            Spacecraft s = spacecraftRepository.findById(e.getSpacecraftId()).orElse(null);
            if (s == null || !isOperativa(s)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", e.getId());
            item.put("spacecraftId", s.getId());
            item.put("spacecraftName", s.getName());
            item.put("franchise", s.getFranchise());
            item.put("eventType", e.getEventType());
            item.put("startDate", e.getStartDate());
            item.put("endDate", e.getEndDate());
            item.put("time", e.getTime());
            item.put("ticketPrice", ticketPriceOf(s));
            theaterEvents.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("museums", museums);
        result.put("theaterEvents", theaterEvents);
        return result;
    }

    private boolean isOperativa(Spacecraft s) {
        return s.getStatus() == null || s.getStatus() == SpacecraftStatus.OPERATIVA;
    }

    private double ticketPriceOf(Spacecraft s) {
        return s.getTicketPrice() != null ? s.getTicketPrice() : BankInPaymentService.DEFAULT_TICKET_PRICE;
    }
}
