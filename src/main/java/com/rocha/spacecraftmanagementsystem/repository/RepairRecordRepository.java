package com.rocha.spacecraftmanagementsystem.repository;

import com.rocha.spacecraftmanagementsystem.model.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {

    List<RepairRecord> findBySpacecraftIdOrderBySentAtDesc(Long spacecraftId);

    // El registro abierto (sin finishedAt) de una nave, si esta en el taller
    Optional<RepairRecord> findBySpacecraftIdAndFinishedAtIsNull(Long spacecraftId);
}