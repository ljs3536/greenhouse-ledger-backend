package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.Plant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, Long> {
	Optional<Plant> findByName(String name);
}
