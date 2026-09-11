package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.MuseumSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MuseumScheduleRepository extends JpaRepository<MuseumSchedule, Long> {

    List<MuseumSchedule> findBySpacecraftIdOrderByDateAsc(Long spacecraftId);

    Optional<MuseumSchedule> findBySpacecraftIdAndDate(Long spacecraftId, LocalDate date);
}