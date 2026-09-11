package com.rocha.spacecraftmanagementsystem.service;

import com.rocha.spacecraftmanagementsystem.exception.SpacecraftNotFoundException;
import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import com.rocha.spacecraftmanagementsystem.model.Spacecraft;
import com.rocha.spacecraftmanagementsystem.repository.MuseumScheduleRepository;
import com.rocha.spacecraftmanagementsystem.repository.SpacecraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MuseumScheduleService {

    private static final int WINDOW_DAYS = 8; // hoy inclusive + 7 dias mas

    @Autowired
    private MuseumScheduleRepository museumScheduleRepository;

    @Autowired
    private SpacecraftRepository spacecraftRepository;

    public MuseumSchedule saveSchedule(MuseumSchedule schedule) {
        Spacecraft spacecraft = spacecraftRepository.findById(schedule.getSpacecraftId())
                .orElseThrow(() -> new SpacecraftNotFoundException(
                        "Spacecraft with ID " + schedule.getSpacecraftId() + " not found"));

        if (!Boolean.TRUE.equals(spacecraft.getIsMuseum())) {
            throw new IllegalArgumentException("Spacecraft " + spacecraft.getId() + " is not a museum");
        }

        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(WINDOW_DAYS - 1);
        if (schedule.getDate() == null || schedule.getDate().isBefore(today) || schedule.getDate().isAfter(limit)) {
            throw new IllegalArgumentException(
                    "date must be between " + today + " and " + limit + " (today plus " + (WINDOW_DAYS - 1) + " days)");
        }

        if (schedule.getOpenTime() == null || schedule.getCloseTime() == null
                || !schedule.getCloseTime().isAfter(schedule.getOpenTime())) {
            throw new IllegalArgumentException("closeTime must be after openTime");
        }

        return museumScheduleRepository.findBySpacecraftIdAndDate(schedule.getSpacecraftId(), schedule.getDate())
                .map(existing -> {
                    existing.setOpenTime(schedule.getOpenTime());
                    existing.setCloseTime(schedule.getCloseTime());
                    return museumScheduleRepository.save(existing);
                })
                .orElseGet(() -> museumScheduleRepository.save(schedule));
    }

    public List<MuseumSchedule> getSchedule(Long spacecraftId) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(WINDOW_DAYS - 1);
        return museumScheduleRepository.findBySpacecraftIdOrderByDateAsc(spacecraftId).stream()
                .filter(s -> !s.getDate().isBefore(today) && !s.getDate().isAfter(limit))
                .toList();
    }
}