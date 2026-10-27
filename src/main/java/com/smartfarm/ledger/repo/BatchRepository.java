package com.smartfarm.ledger.repo;

import com.smartfarm.ledger.domain.Batch;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchRepository extends JpaRepository<Batch, Long> {

	@Query("""
			select b from Batch b
			join fetch b.greenhouse
			join fetch b.plant
			order by b.plantedOn desc, b.id desc
			""")
	List<Batch> findAllWithDetails();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select b from Batch b
			join fetch b.greenhouse
			join fetch b.plant
			where b.id = :id
			""")
	Optional<Batch> lockById(@Param("id") Long id);

	@Query("""
			select b from Batch b
			join fetch b.greenhouse
			join fetch b.plant
			where b.id = :id
			""")
	Optional<Batch> findWithDetails(@Param("id") Long id);

	boolean existsByGreenhouse_Id(Long greenhouseId);

	boolean existsByPlant_Id(Long plantId);
}
