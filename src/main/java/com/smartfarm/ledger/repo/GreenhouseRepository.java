package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.Greenhouse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreenhouseRepository extends JpaRepository<Greenhouse, Long> {
	Optional<Greenhouse> findByName(String name);
}
