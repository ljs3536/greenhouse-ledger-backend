package com.smartfarm.ledger.service;

import com.smartfarm.ledger.domain.Batch;
import com.smartfarm.ledger.domain.House;
import com.smartfarm.ledger.domain.Movement;
import com.smartfarm.ledger.domain.MovementType;
import com.smartfarm.ledger.domain.Stage;
import com.smartfarm.ledger.domain.StageChange;
import com.smartfarm.ledger.repo.BatchRepository;
import com.smartfarm.ledger.repo.HouseRepository;
import com.smartfarm.ledger.repo.MovementRepository;
import com.smartfarm.ledger.repo.StageChangeRepository;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchDetailResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.HistoryItem;
import com.smartfarm.ledger.web.dto.LedgerDtos.HouseSummary;
import com.smartfarm.ledger.web.dto.LedgerDtos.SummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final HouseRepository houses;
	private final BatchRepository batches;
	private final MovementRepository movements;
	private final StageChangeRepository stageChanges;

	public LedgerService(
			HouseRepository houses,
			BatchRepository batches,
			MovementRepository movements,
			StageChangeRepository stageChanges) {
		this.houses = houses;
		this.batches = batches;
		this.movements = movements;
		this.stageChanges = stageChanges;
	}

	@Transactional(readOnly = true)
	public SummaryResponse summary() {
		Map<Long, Long> quantities = quantitiesByBatch();
		List<Batch> allBatches = batches.findAllWithHouse();
		List<HouseSummary> houseSummaries = houses.findAll().stream()
				.sorted(Comparator.comparing(House::getId))
				.map(house -> summarize(house, allBatches, quantities))
				.toList();
		return new SummaryResponse(
				houseSummaries,
				houseSummaries.stream().mapToLong(HouseSummary::totalQuantity).sum(),
				houseSummaries.stream().mapToLong(HouseSummary::sellableQuantity).sum(),
				houseSummaries.stream().mapToLong(HouseSummary::batchCount).sum());
	}

	@Transactional(readOnly = true)
	public List<BatchResponse> listBatches() {
		Map<Long, Long> quantities = quantitiesByBatch();
		return batches.findAllWithHouse().stream()
				.map(batch -> toResponse(batch, quantities.getOrDefault(batch.getId(), 0L)))
				.toList();
	}

	@Transactional(readOnly = true)
	public BatchDetailResponse batch(long id) {
		Batch batch = batches.findWithHouse(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "배치를 찾을 수 없습니다."));
		long quantity = number(movements.quantityOf(id));
		List<Stamp> history = new ArrayList<>();
		for (Movement movement : movements.findByBatchIdOrderByOccurredOnDescIdDesc(id)) {
			history.add(new Stamp(
					new HistoryItem(
							"m-" + movement.getId(),
							"movement",
							movement.getOccurredOn(),
							movement.getMemo(),
							movement.getType(),
							movement.getQuantity(),
							null,
							null),
					movement.getCreatedAt(),
					movement.getId()));
		}
		for (StageChange change : stageChanges.findByBatchIdOrderByChangedOnDescIdDesc(id)) {
			history.add(new Stamp(
					new HistoryItem(
							"s-" + change.getId(),
							"stage",
							change.getChangedOn(),
							null,
							null,
							null,
							change.getFromStage(),
							change.getToStage()),
					change.getCreatedAt(),
					change.getId()));
		}
		history.sort(Comparator
				.comparing((Stamp stamp) -> stamp.item().date()).reversed()
				.thenComparing(stamp -> stamp.createdAt() == null ? LocalDateTime.MIN : stamp.createdAt(), Comparator.reverseOrder())
				.thenComparing(Stamp::seq, Comparator.reverseOrder()));
		BatchResponse response = toResponse(batch, quantity);
		return new BatchDetailResponse(
				response.id(),
				response.houseId(),
				response.houseName(),
				response.cropCode(),
				response.cropName(),
				response.plantedOn(),
				response.stage(),
				response.note(),
				response.quantity(),
				history.stream().map(Stamp::item).toList());
	}

	@Transactional
	public long createBatch(long houseId, LocalDate plantedOn, Stage stage, int quantity, String note) {
		House house = houses.findById(houseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "하우스를 찾을 수 없습니다."));
		Stage initialStage = stage == null ? Stage.SEEDLING : stage;
		String trimmedNote = blankToNull(note);
		Batch batch = batches.save(new Batch(house, plantedOn, initialStage, trimmedNote));
		String memo = trimmedNote == null ? "최초 입고" : "최초 입고 · " + trimmedNote;
		movements.save(new Movement(batch, MovementType.INBOUND, quantity, plantedOn, memo));
		return batch.getId();
	}

	@Transactional
	public void changeStage(long id, Stage stage) {
		Batch batch = batches.lockById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "배치를 찾을 수 없습니다."));
		if (batch.getStage() == stage) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "선택한 단계가 현재 단계와 같습니다.");
		}
		Stage previous = batch.getStage();
		batch.changeStage(stage);
		stageChanges.save(new StageChange(batch, previous, stage, LocalDate.now(SEOUL)));
	}

	@Transactional
	public void addMovement(long id, MovementType type, int quantity, LocalDate occurredOn, String memo) {
		Batch batch = batches.lockById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "배치를 찾을 수 없습니다."));
		if (type == MovementType.SALE && batch.getStage() != Stage.READY) {
			throw new ApiException(HttpStatus.CONFLICT, "판매 출고는 출하 가능 단계에서 기록합니다.");
		}
		long available = number(movements.quantityOf(id));
		if (type != MovementType.INBOUND && quantity > available) {
			throw new ApiException(HttpStatus.CONFLICT, "현재 " + available + "포트입니다. " + available + "포트 이하로 기록할 수 있습니다.");
		}
		movements.save(new Movement(batch, type, quantity, occurredOn, blankToNull(memo)));
	}

	@Transactional
	public void deleteBatch(long id) {
		if (!batches.existsById(id)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "배치를 찾을 수 없습니다.");
		}
		batches.deleteById(id);
	}

	private HouseSummary summarize(House house, List<Batch> allBatches, Map<Long, Long> quantities) {
		long batchCount = 0;
		long total = 0;
		long sellable = 0;
		for (Batch batch : allBatches) {
			if (!batch.getHouse().getId().equals(house.getId())) {
				continue;
			}
			long quantity = quantities.getOrDefault(batch.getId(), 0L);
			batchCount += 1;
			total += quantity;
			if (batch.getStage() == Stage.READY) {
				sellable += quantity;
			}
		}
		return new HouseSummary(
				house.getId(),
				house.getCode(),
				house.getName(),
				house.getCropCode(),
				house.getCropName(),
				house.getDayTemp(),
				house.getNightTemp(),
				house.getLightNote(),
				batchCount,
				total,
				sellable);
	}

	private Map<Long, Long> quantitiesByBatch() {
		Map<Long, Long> quantities = new HashMap<>();
		for (Object[] row : movements.quantityByBatch()) {
			quantities.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		}
		return quantities;
	}

	private BatchResponse toResponse(Batch batch, long quantity) {
		House house = batch.getHouse();
		return new BatchResponse(
				batch.getId(),
				house.getId(),
				house.getName(),
				house.getCropCode(),
				house.getCropName(),
				batch.getPlantedOn(),
				batch.getStage(),
				batch.getNote(),
				quantity);
	}

	private static long number(Number value) {
		return value == null ? 0 : value.longValue();
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private record Stamp(HistoryItem item, LocalDateTime createdAt, long seq) {
	}
}
