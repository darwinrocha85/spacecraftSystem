package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.service.MuseumScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/museum-schedules")
public class MuseumScheduleController {

    @Autowired
    private MuseumScheduleService museumScheduleService;

    // Crea o actualiza el horario de un dia puntual (spacecraftId + date son la clave)
    @PostMapping
    public ResponseEntity<?> saveSchedule(@RequestBody MuseumSchedule schedule) {
        try {
            MuseumSchedule saved = museumScheduleService.saveSchedule(schedule);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Horario vigente (hoy a hoy+6) de una nave museo
    @GetMapping
    public List<MuseumSchedule> getSchedule(@RequestParam Long spacecraftId) {
        return museumScheduleService.getSchedule(spacecraftId);
    }
}