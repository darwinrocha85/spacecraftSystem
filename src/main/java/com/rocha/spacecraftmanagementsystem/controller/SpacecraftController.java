package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.service.SpacecraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    // Obtener todas las naves espaciales con paginación
    @GetMapping("/page")
    public Page<Spacecraft> getAllSpacecrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {

        return spacecraftService.findAllPage(page, size, sortBy, sortDirection);
    }

    // Obtener una nave espacial por ID
    @Cacheable(value = "spacecrafts", key = "#id")
    @GetMapping("/{id}")
    public ResponseEntity<Spacecraft> getSpacecraftById(@PathVariable Long id) {
        return spacecraftService.getSpacecraftById(id);
    }

    // Crear una nueva nave espacial
    @PostMapping
    public ResponseEntity<Spacecraft> createSpacecraft(@RequestBody Spacecraft spacecraft) {

        return spacecraftService.createSpacecraft(spacecraft);

    }

    // Actualizar una nave espacial existente
    @PutMapping("/{id}")
    public ResponseEntity<Spacecraft> updateSpacecraft(@PathVariable Long id, @RequestBody Spacecraft spacecraftDetails) {
        return spacecraftService.updateSpacecraft(id, spacecraftDetails);

    }

    // Eliminar una nave espacial por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpacecraft(@PathVariable Long id) {
       return spacecraftService.deleteSpacecraft(id);
    }

    // Obtener todas las naves espaciales que contienen un texto en su nombre con paginación
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
