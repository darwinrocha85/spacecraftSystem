package com.rocha.spacecraftmanagementsystem.serviceTest;

import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpacecraftService {

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    // Método para obtener una nave espacial por ID
    public Spacecraft getSpacecraftById(Long id) {
        return spacecraftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spacecraft not found with ID: " + id));
    }
}
