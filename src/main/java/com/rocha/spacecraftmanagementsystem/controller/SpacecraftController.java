package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private SpacecraftRepository spacecraftRepository;

    // Obtener todas las naves espaciales
    @GetMapping
    public List<Spacecraft> getAllSpacecrafts() {
        return spacecraftRepository.findAll();
    }

    // Obtener todas las naves espaciales con paginación
    @GetMapping("/page")
    public Page<Spacecraft> getAllSpacecrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return spacecraftRepository.findAll(pageable);
    }

    // Obtener una nave espacial por ID
    @GetMapping("/{id}")
    public ResponseEntity<Spacecraft> getSpacecraftById(@PathVariable Long id) {
        Optional<Spacecraft> spacecraft = spacecraftRepository.findById(id);
        return spacecraft.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Crear una nueva nave espacial
    @PostMapping
    public ResponseEntity<Spacecraft> createSpacecraft(@RequestBody Spacecraft spacecraft) {
        Spacecraft savedSpacecraft = spacecraftRepository.save(spacecraft);
        return new ResponseEntity<>(savedSpacecraft, HttpStatus.CREATED);
    }

    // Actualizar una nave espacial existente
    @PutMapping("/{id}")
    public ResponseEntity<Spacecraft> updateSpacecraft(@PathVariable Long id, @RequestBody Spacecraft spacecraftDetails) {
        Optional<Spacecraft> optionalSpacecraft = spacecraftRepository.findById(id);

        if (optionalSpacecraft.isPresent()) {
            Spacecraft spacecraft = optionalSpacecraft.get();
            spacecraft.setName(spacecraftDetails.getName());
            spacecraft.setFranchise(spacecraftDetails.getFranchise());
            spacecraft.setCrewCapacity(spacecraftDetails.getCrewCapacity());
            spacecraft.setSpeed(spacecraftDetails.getSpeed());
            spacecraft.setSpacecraftType(spacecraftDetails.getSpacecraftType());
            spacecraft.setIsArmed(spacecraftDetails.getIsArmed());

            Spacecraft updatedSpacecraft = spacecraftRepository.save(spacecraft);
            return ResponseEntity.ok(updatedSpacecraft);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Eliminar una nave espacial por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpacecraft(@PathVariable Long id) {
        if (spacecraftRepository.existsById(id)) {
            spacecraftRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
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
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return spacecraftRepository.findByNameContainingIgnoreCase(name, pageable);
    }
}
