package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {
}
