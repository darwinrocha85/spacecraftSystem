package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.model.ClosedTheaterEvent;
import com.rocha.spacecraftmanagementsystem.model.RepairDamage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Fase 1 (extraccion del taller a backend Python): avisa a spacecraft-taller-backend cuando una
// nave fue enviada a reparar. spacecraftSystem sigue orquestando "enviar a taller" (valida,
// cancela entradas activas y revierte sus cobros con BankIn - logica sin tocar) y, recien
// DESPUES de que esa cancelacion ya quedo confirmada, llama a este cliente para que el taller
// (Python) cree su propio registro de reparacion con el detalle de danos.
//
// Best-effort a proposito: si esta llamada falla (Python caido, timeout, TALLER_API_BASE mal
// apuntado), NO se revierte la cancelacion de entradas ya hecha - eso ya esta confirmado y no
// tiene vuelta atras. Solo se loguea y se devuelve false para que RepairService pueda avisarle
// al admin con un aviso en la respuesta. El endpoint interno de taller es idempotente (no
// duplica si ya hay una reparacion activa para esa nave), asi que un reintento manual futuro
// es seguro.
@Service
public class TallerBackendClient {

    private static final Logger logger = LoggerFactory.getLogger(TallerBackendClient.class);

    @Value("${taller.api.base}")
    private String apiBase;

    @Value("${taller.api.internal-key:}")
    private String internalApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean notifyRepairCreated(Long spacecraftId, String spacecraftName, String spacecraftModel,
                                        List<RepairDamage> damages, List<LocalDate> closedMuseumDates,
                                        List<ClosedTheaterEvent> closedTheaterEvents) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Api-Key", internalApiKey);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("spacecraftId", spacecraftId);
        body.put("spacecraftName", spacecraftName);
        body.put("spacecraftModel", spacecraftModel);

        List<Map<String, String>> damagePayload = new ArrayList<>();
        for (RepairDamage d : damages) {
            damagePayload.add(Map.of(
                    "category", d.getCategory().name(),
                    "subtype", d.getSubtype()));
        }
        body.put("damages", damagePayload);

        List<String> closedDatesPayload = new ArrayList<>();
        for (LocalDate date : closedMuseumDates) {
            closedDatesPayload.add(date.toString());
        }
        body.put("closedMuseumDates", closedDatesPayload);

        List<String> closedEventsPayload = new ArrayList<>();
        for (ClosedTheaterEvent event : closedTheaterEvents) {
            closedEventsPayload.add(
                    event.getEventType() + " " + event.getStartDate() + " a " + event.getEndDate()
                            + " " + event.getEventTime());
        }
        body.put("closedTheaterEvents", closedEventsPayload);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiBase + "/api/internal/repairs", entity, String.class);
            logger.info("Backend de taller notificado para la nave {}: {}", spacecraftId, response.getStatusCode());
            return true;
        } catch (RestClientException e) {
            logger.error(
                    "No se pudo notificar al backend de taller sobre la nave {} (la cancelacion de entradas "
                            + "ya quedo confirmada del lado de spacecraftSystem, no se revierte): {}",
                    spacecraftId, e.getMessage());
            return false;
        }
    }
}
