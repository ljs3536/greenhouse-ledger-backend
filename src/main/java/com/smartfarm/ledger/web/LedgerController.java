package com.smartfarm.ledger.web;

import com.smartfarm.ledger.service.LedgerService;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchDetailResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.CreateBatchRequest;
import com.smartfarm.ledger.web.dto.LedgerDtos.IdResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.MovementRequest;
import com.smartfarm.ledger.web.dto.LedgerDtos.OkResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.StageRequest;
import com.smartfarm.ledger.web.dto.LedgerDtos.SummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LedgerController {

	private final LedgerService ledger;

	public LedgerController(LedgerService ledger) {
		this.ledger = ledger;
	}

	@GetMapping("/health")
	Map<String, Boolean> health() {
		return Map.of("ok", true);
	}

	@GetMapping("/summary")
	SummaryResponse summary() {
		return ledger.summary();
	}

	@GetMapping("/batches")
	List<BatchResponse> batches() {
		return ledger.listBatches();
	}

	@GetMapping("/batches/{id}")
	BatchDetailResponse batch(@PathVariable long id) {
		return ledger.batch(id);
	}

	@PostMapping("/batches")
	@ResponseStatus(HttpStatus.CREATED)
	IdResponse create(@Valid @RequestBody CreateBatchRequest request) {
		long id = ledger.createBatch(
				request.houseId(),
				request.plantedOn(),
				request.stage(),
				request.quantity(),
				request.note());
		return new IdResponse(id);
	}

	@PatchMapping("/batches/{id}/stage")
	OkResponse changeStage(@PathVariable long id, @Valid @RequestBody StageRequest request) {
		ledger.changeStage(id, request.stage());
		return new OkResponse(true);
	}

	@PostMapping("/batches/{id}/movements")
	@ResponseStatus(HttpStatus.CREATED)
	OkResponse addMovement(@PathVariable long id, @Valid @RequestBody MovementRequest request) {
		ledger.addMovement(id, request.type(), request.quantity(), request.occurredOn(), request.memo());
		return new OkResponse(true);
	}

	@DeleteMapping("/batches/{id}")
	OkResponse delete(@PathVariable long id) {
		ledger.deleteBatch(id);
		return new OkResponse(true);
	}
}
