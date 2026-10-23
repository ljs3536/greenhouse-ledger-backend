package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.StageChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageChangeRepository extends JpaRepository<StageChange, Long> {
	List<StageChange> findByBatchIdOrderByChangedOnDescIdDesc(Long batchId);
}
