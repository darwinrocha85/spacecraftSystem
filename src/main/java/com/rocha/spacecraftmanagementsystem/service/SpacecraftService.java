package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class SpacecraftService {

    @Autowired
    private SpacecraftRepository spacecraftRepository;


    public List<Spacecraft> getAllSpacecrafts() {
        return spacecraftRepository.findAll();
    }



    public Page<Spacecraft> findAllPage(
             int page,
             int size,
             String sortBy,
             String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return spacecraftRepository.findAll(pageable);
    }

    // Obtener una nave espacial por ID
    @Cacheable(value = "spacecrafts", key = "#id")
    public ResponseEntity<Spacecraft> getSpacecraftById(Long id) {
        Spacecraft spacecraft = spacecraftRepository.findById(id)
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + id + " not found"));
        return ResponseEntity.ok(spacecraft);
    }

    public Spacecraft getSpacecraftById2(Long id) {
        return spacecraftRepository.findById(id)
                .orElseThrow(() -> new SpacecraftNotFoundException("Spacecraft with ID " + id + " not found"));
    }


    public ResponseEntity<Spacecraft> createSpacecraft(@RequestBody Spacecraft spacecraft) {
        Spacecraft savedSpacecraft = spacecraftRepository.save(spacecraft);
        return new ResponseEntity<>(savedSpacecraft, HttpStatus.CREATED);
    }

    @CacheEvict(value = "spacecrafts", key = "#id")
    public ResponseEntity<Spacecraft> updateSpacecraft(Long id, Spacecraft spacecraftDetails) {
        Optional<Spacecraft> optionalSpacecraft = spacecraftRepository.findById(id);

        if (optionalSpacecraft.isPresent()) {
            Spacecraft spacecraft = optionalSpacecraft.get();
            spacecraft.setName(spacecraftDetails.getName());
            spacecraft.setFranchise(spacecraftDetails.getFranchise());
            spacecraft.setCrewCapacity(spacecraftDetails.getCrewCapacity());
            spacecraft.setSpeed(spacecraftDetails.getSpeed());
            spacecraft.setSpacecraftType(spacecraftDetails.getSpacecraftType());


            Spacecraft updatedSpacecraft = spacecraftRepository.save(spacecraft);
            return ResponseEntity.ok(updatedSpacecraft);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @CacheEvict(value = "spacecrafts", key = "#id")
    public ResponseEntity<Void> deleteSpacecraft(@PathVariable Long id) {
        if (spacecraftRepository.existsById(id)) {
            spacecraftRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    public Page<Spacecraft> searchSpacecraftsByName(
            String name,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return spacecraftRepository.findByNameContainingIgnoreCase(name, pageable);
    }
}
