package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.Movement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {

	List<Movement> findByBatchIdOrderByOccurredOnDescIdDesc(Long batchId);

	@Query(value = """
			SELECT COALESCE(SUM(CASE WHEN type = 'INBOUND' THEN quantity ELSE -quantity END), 0)
			FROM movements
			WHERE batch_id = :batchId
			""", nativeQuery = true)
	Number quantityOf(@Param("batchId") long batchId);

	@Query(value = """
			SELECT batch_id, COALESCE(SUM(CASE WHEN type = 'INBOUND' THEN quantity ELSE -quantity END), 0)
			FROM movements
			GROUP BY batch_id
			""", nativeQuery = true)
	List<Object[]> quantityByBatch();
}
