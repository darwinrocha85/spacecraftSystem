package com.rocha.spacecraftmanagementsystem.controller;

import com.rocha.spacecraftmanagementsystem.service.MarketingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Fase 5: API publica de solo lectura que consume el landing de marketing
// (spacecraft-events-landing) para listar lo que hoy se puede reservar.
@RestController
@RequestMapping("/api/marketing")
public class MarketingController {

    @Autowired
    private MarketingService marketingService;

    // Museos operativos + funciones de teatro vigentes, listos para mostrar en el landing
    @GetMapping("/experiences")
    public Map<String, Object> getExperiences() {
        return marketingService.getExperiences();
    }
}
