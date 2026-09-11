package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.service.SpacecraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spacecrafts")
public class SpacecraftController {

    @Autowired
    private SpacecraftService spacecraftService;

    // Obtener todas las naves espaciales
    @GetMapping
    public List<Spacecraft> getAllSpacecrafts() {
        return spacecraftService.getAllSpacecrafts();
    }

    // Obtener todas las naves espaciales con paginacion
    @GetMapping("/page")
    public Page<Spacecraft> getAllSpacecrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {

        return spacecraftService.findAllPage(page, size, sortBy, sortDirection);
    }

    // Naves habilitadas como museo y/o teatro (las usara la app de entradas)
    @GetMapping("/venues")
    public List<Spacecraft> getVenues(@RequestParam(required = false) String type) {
        return spacecraftService.getVenues(type);
    }

    // Obtener una nave espacial por ID
    @Cacheable(value = "spacecrafts", key = "#id")
    @GetMapping("/{id}")
    public ResponseEntity<Spacecraft> getSpacecraftById(@PathVariable Long id) {
        return spacecraftService.getSpacecraftById(id);
    }

    // Crear una nueva nave espacial
    @PostMapping
    public ResponseEntity<?> createSpacecraft(@RequestBody Spacecraft spacecraft) {
        try {
            return spacecraftService.createSpacecraft(spacecraft);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Actualizar una nave espacial existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSpacecraft(@PathVariable Long id, @RequestBody Spacecraft spacecraftDetails) {
        try {
            return spacecraftService.updateSpacecraft(id, spacecraftDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Eliminar una nave espacial por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpacecraft(@PathVariable Long id) {
       return spacecraftService.deleteSpacecraft(id);
    }

    // Obtener todas las naves espaciales que contienen un texto en su nombre con paginacion
    @GetMapping("/search")
    public Page<Spacecraft> searchSpacecraftsByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return spacecraftService.searchSpacecraftsByName(name, page, size, sortBy, sortDirection);
    }
}