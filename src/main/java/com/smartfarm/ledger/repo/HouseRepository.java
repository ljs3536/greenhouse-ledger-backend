package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.House;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseRepository extends JpaRepository<House, Long> {
	Optional<House> findByCode(String code);
}
