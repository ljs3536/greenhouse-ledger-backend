package com.smartfarm.ledger.service;

import com.smartfarm.ledger.domain.Batch;
import com.smartfarm.ledger.domain.Greenhouse;
import com.smartfarm.ledger.domain.GreenhouseControl;
import com.smartfarm.ledger.domain.SensorReading;
import com.smartfarm.ledger.domain.Movement;
import com.smartfarm.ledger.domain.MovementType;
import com.smartfarm.ledger.domain.Plant;
import com.smartfarm.ledger.domain.Stage;
import com.smartfarm.ledger.domain.StageChange;
import com.smartfarm.ledger.repo.BatchRepository;
import com.smartfarm.ledger.repo.GreenhouseControlRepository;
import com.smartfarm.ledger.repo.GreenhouseRepository;
import com.smartfarm.ledger.repo.MovementRepository;
import com.smartfarm.ledger.repo.PlantRepository;
import com.smartfarm.ledger.repo.SensorReadingRepository;
import com.smartfarm.ledger.repo.StageChangeRepository;
import com.smartfarm.ledger.web.dto.LedgerDtos.AttentionItem;
import com.smartfarm.ledger.web.dto.LedgerDtos.ClimateAlert;
import com.smartfarm.ledger.web.dto.LedgerDtos.ControlSuggestion;
import com.smartfarm.ledger.web.dto.LedgerDtos.ControlsRequest;
import com.smartfarm.ledger.web.dto.LedgerDtos.ControlsResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.GreenhouseControlState;
import com.smartfarm.ledger.web.dto.LedgerDtos.GreenhouseDetailResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchDetailResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.BatchResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.GreenhouseSummary;
import com.smartfarm.ledger.web.dto.LedgerDtos.HistoryItem;
import com.smartfarm.ledger.web.dto.LedgerDtos.PlantSummary;
import com.smartfarm.ledger.web.dto.LedgerDtos.SensorReadingResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.StageQuantity;
import com.smartfarm.ledger.web.dto.LedgerDtos.SummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final GreenhouseRepository greenhouses;
	private final PlantRepository plants;
	private final BatchRepository batches;
	private final MovementRepository movements;
	private final StageChangeRepository stageChanges;
	private final SensorReadingRepository readings;
	private final GreenhouseControlRepository controls;

	public LedgerService(
			GreenhouseRepository greenhouses,
			PlantRepository plants,
			BatchRepository batches,
			MovementRepository movements,
			StageChangeRepository stageChanges,
			SensorReadingRepository readings,
			GreenhouseControlRepository controls) {
		this.greenhouses = greenhouses;
		this.plants = plants;
		this.batches = batches;
		this.movements = movements;
		this.stageChanges = stageChanges;
		this.readings = readings;
		this.controls = controls;
	}

	@Transactional(readOnly = true)
	public SummaryResponse summary() {
		Map<Long, Long> quantities = quantitiesByBatch();
		List<Batch> allBatches = batches.findAllWithDetails();
		List<GreenhouseSummary> greenhouseSummaries = greenhouses.findAll().stream()
				.sorted(Comparator.comparing(Greenhouse::getId))
				.map(greenhouse -> summarizeGreenhouse(greenhouse, allBatches, quantities))
				.toList();
		List<PlantSummary> plantSummaries = plants.findAll().stream()
				.sorted(Comparator.comparing(Plant::getName))
				.map(plant -> summarizePlant(plant, allBatches, quantities))
				.toList();
		long total = 0;
		long sellable = 0;
		Map<Stage, Long> stageQuantities = new HashMap<>();
		for (Stage stage : Stage.values()) {
			stageQuantities.put(stage, 0L);
		}
		for (Batch batch : allBatches) {
			long quantity = quantities.getOrDefault(batch.getId(), 0L);
			total += quantity;
			stageQuantities.merge(batch.getStage(), quantity, Long::sum);
			if (batch.getStage() == Stage.READY) {
				sellable += quantity;
			}
		}
		List<StageQuantity> stages = new ArrayList<>();
		for (Stage stage : Stage.values()) {
			stages.add(new StageQuantity(stage, stageQuantities.get(stage)));
		}
		List<AttentionItem> attention = attention(allBatches, quantities);
		List<ClimateAlert> climate = climate(allBatches, quantities);
		return new SummaryResponse(attention, climate, greenhouseSummaries, plantSummaries, stages, total, sellable, allBatches.size());
	}

	private List<ClimateAlert> climate(List<Batch> allBatches, Map<Long, Long> quantities) {
		Map<Long, SensorReading> byGreenhouse = readings.findAll().stream()
				.collect(Collectors.toMap(SensorReading::getGreenhouseId, Function.identity()));
		boolean day = isDaytime(LocalTime.now(SEOUL));
		Set<String> seen = new HashSet<>();
		List<ClimateAlert> alerts = new ArrayList<>();
		for (Batch batch : allBatches) {
			if (quantities.getOrDefault(batch.getId(), 0L) <= 0) {
				continue;
			}
			long greenhouseId = batch.getGreenhouse().getId();
			long plantId = batch.getPlant().getId();
			if (!seen.add(greenhouseId + ":" + plantId)) {
				continue;
			}
			SensorReading reading = byGreenhouse.get(greenhouseId);
			if (reading == null) {
				continue;
			}
			Plant plant = batch.getPlant();
			Greenhouse greenhouse = batch.getGreenhouse();
			BigDecimal tempMin = day ? plant.getDayTempMin() : plant.getNightTempMin();
			BigDecimal tempMax = day ? plant.getDayTempMax() : plant.getNightTempMax();
			if (outside(reading.getTemperature(), tempMin, tempMax)) {
				alerts.add(alert(greenhouse, plant, day ? "낮 온도" : "밤 온도", formatTemp(reading.getTemperature()), formatRange(tempMin, tempMax, "°C")));
			}
			if (outside(reading.getHumidity(), plant.getHumidityMin(), plant.getHumidityMax())) {
				alerts.add(alert(greenhouse, plant, "습도", reading.getHumidity() + "%", formatRange(plant.getHumidityMin(), plant.getHumidityMax(), "%")));
			}
			if (outside(reading.getLight(), plant.getLightMin(), plant.getLightMax())) {
				alerts.add(alert(greenhouse, plant, "광", formatLight(reading.getLight()), formatRange(plant.getLightMin(), plant.getLightMax(), "lx")));
			}
		}
		alerts.sort(Comparator
				.comparing(ClimateAlert::greenhouseId)
				.thenComparing(ClimateAlert::plantName)
				.thenComparingInt(item -> labelOrder(item.label())));
		return alerts;
	}

	private static boolean isDaytime(LocalTime time) {
		return !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(18, 0));
	}

	private static ClimateAlert alert(Greenhouse greenhouse, Plant plant, String label, String value, String range) {
		return new ClimateAlert(greenhouse.getId(), greenhouse.getName(), plant.getId(), plant.getName(), label, value, range);
	}

	private static boolean outside(BigDecimal value, BigDecimal min, BigDecimal max) {
		if (min == null && max == null) {
			return false;
		}
		if (min != null && value.compareTo(min) < 0) {
			return true;
		}
		return max != null && value.compareTo(max) > 0;
	}

	private static boolean outside(int value, Integer min, Integer max) {
		if (min == null && max == null) {
			return false;
		}
		if (min != null && value < min) {
			return true;
		}
		return max != null && value > max;
	}

	private static String formatTemp(BigDecimal value) {
		return value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "°C";
	}

	private static String formatLight(int value) {
		return String.format(Locale.KOREA, "%,d", value) + "lx";
	}

	private static String formatRange(BigDecimal min, BigDecimal max, String unit) {
		String low = min == null ? null : min.setScale(1, RoundingMode.HALF_UP).toPlainString();
		String high = max == null ? null : max.setScale(1, RoundingMode.HALF_UP).toPlainString();
		return joinRange(low, high, unit);
	}

	private static String formatRange(Integer min, Integer max, String unit) {
		String low = min == null ? null : String.format(Locale.KOREA, "%,d", min);
		String high = max == null ? null : String.format(Locale.KOREA, "%,d", max);
		return joinRange(low, high, unit);
	}

	private static String joinRange(String min, String max, String unit) {
		if (min != null && max != null) {
			return min + "–" + max + unit;
		}
		if (min != null) {
			return min + unit + " 이상";
		}
		return max + unit + " 이하";
	}

	private static int labelOrder(String label) {
		return switch (label) {
			case "낮 온도", "밤 온도" -> 0;
			case "습도" -> 1;
			default -> 2;
		};
	}

	private List<AttentionItem> attention(List<Batch> allBatches, Map<Long, Long> quantities) {
		Map<Long, StageChange> latestChange = new HashMap<>();
		for (StageChange change : stageChanges.findAllByOrderByChangedOnDescIdDesc()) {
			latestChange.putIfAbsent(change.getBatch().getId(), change);
		}
		LocalDate today = LocalDate.now(SEOUL);
		List<AttentionItem> items = new ArrayList<>();
		for (Batch batch : allBatches) {
			long quantity = quantities.getOrDefault(batch.getId(), 0L);
			if (quantity <= 0) {
				continue;
			}
			StageChange change = latestChange.get(batch.getId());
			LocalDate enteredOn = change != null && change.getToStage() == batch.getStage()
					? change.getChangedOn()
					: batch.getPlantedOn();
			int daysInStage = (int) Math.max(0, ChronoUnit.DAYS.between(enteredOn, today));
			Integer referenceDays = batch.getPlant().referenceDays(batch.getStage());
			boolean ready = batch.getStage() == Stage.READY;
			boolean overdue = referenceDays != null && daysInStage >= referenceDays;
			if (!ready && !overdue) {
				continue;
			}
			items.add(new AttentionItem(
					batch.getId(),
					batch.getGreenhouse().getId(),
					batch.getGreenhouse().getName(),
					batch.getPlant().getName(),
					batch.getStage(),
					quantity,
					daysInStage,
					ready ? null : referenceDays));
		}
		items.sort(Comparator
				.comparing((AttentionItem item) -> item.stage() == Stage.READY)
				.thenComparing((AttentionItem item) -> item.stage() == Stage.READY
						? -item.quantity()
						: -(item.daysInStage() - item.referenceDays())));
		return items;
	}

	@Transactional(readOnly = true)
	public GreenhouseDetailResponse greenhouse(long id) {
		Greenhouse greenhouse = greenhouses.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다."));
		Map<Long, Long> quantities = quantitiesByBatch();
		List<BatchResponse> houseBatches = batches.findAllWithDetails().stream()
				.filter(batch -> batch.getGreenhouse().getId().equals(id))
				.map(batch -> toResponse(batch, quantities.getOrDefault(batch.getId(), 0L)))
				.toList();
		SensorReadingResponse sensor = readings.findById(id)
				.map(reading -> new SensorReadingResponse(
						reading.getGreenhouseId(),
						greenhouse.getName(),
						reading.getTemperature(),
						reading.getHumidity(),
						reading.getLight(),
						reading.getRecordedAt()))
				.orElse(null);
		return new GreenhouseDetailResponse(
				greenhouse.getId(),
				greenhouse.getName(),
				sensor,
				controlsOf(controls.findById(id).orElse(null)),
				suggestions(id, quantities),
				houseBatches);
	}

	@Transactional(readOnly = true)
	public List<GreenhouseControlState> listControls() {
		Map<Long, GreenhouseControl> saved = controls.findAll().stream()
				.collect(Collectors.toMap(GreenhouseControl::getGreenhouseId, Function.identity()));
		return greenhouses.findAll().stream()
				.sorted(Comparator.comparing(Greenhouse::getId))
				.map(greenhouse -> controlState(greenhouse.getId(), saved.get(greenhouse.getId())))
				.toList();
	}

	@Transactional
	public ControlsResponse updateControls(long id, ControlsRequest request) {
		if (!greenhouses.existsById(id)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다.");
		}
		GreenhouseControl control = controls.findById(id).orElseGet(() -> new GreenhouseControl(id));
		control.update(
				request.heater(),
				request.cooler(),
				request.vent(),
				request.humidifier(),
				request.growLight(),
				request.shade());
		controls.save(control);
		return controlsOf(control);
	}

	private List<ControlSuggestion> suggestions(long greenhouseId, Map<Long, Long> quantities) {
		SensorReading reading = readings.findById(greenhouseId).orElse(null);
		if (reading == null) {
			return List.of();
		}
		boolean day = isDaytime(LocalTime.now(SEOUL));
		Set<Long> seenPlants = new HashSet<>();
		List<ControlSuggestion> items = new ArrayList<>();
		for (Batch batch : batches.findAllWithDetails()) {
			if (!batch.getGreenhouse().getId().equals(greenhouseId) || quantities.getOrDefault(batch.getId(), 0L) <= 0) {
				continue;
			}
			if (!seenPlants.add(batch.getPlant().getId())) {
				continue;
			}
			Plant plant = batch.getPlant();
			BigDecimal tempMin = day ? plant.getDayTempMin() : plant.getNightTempMin();
			BigDecimal tempMax = day ? plant.getDayTempMax() : plant.getNightTempMax();
			String tempLabel = day ? "낮 온도" : "밤 온도";
			if (below(reading.getTemperature(), tempMin)) {
				items.add(suggestion("HEATER", "난방", "온도를 올립니다", plant, tempLabel, formatTemp(reading.getTemperature()), formatRange(tempMin, tempMax, "°C")));
			} else if (above(reading.getTemperature(), tempMax)) {
				items.add(suggestion("COOLER", "냉방", "온도를 내립니다", plant, tempLabel, formatTemp(reading.getTemperature()), formatRange(tempMin, tempMax, "°C")));
			}
			if (below(reading.getHumidity(), plant.getHumidityMin())) {
				items.add(suggestion("HUMIDIFIER", "가습", "습도를 올립니다", plant, "습도", reading.getHumidity() + "%", formatRange(plant.getHumidityMin(), plant.getHumidityMax(), "%")));
			} else if (above(reading.getHumidity(), plant.getHumidityMax())) {
				items.add(suggestion("VENT", "환기", "습도를 내립니다", plant, "습도", reading.getHumidity() + "%", formatRange(plant.getHumidityMin(), plant.getHumidityMax(), "%")));
			}
			if (below(reading.getLight(), plant.getLightMin())) {
				items.add(suggestion("GROW_LIGHT", "보광", "광을 올립니다", plant, "광", formatLight(reading.getLight()), formatRange(plant.getLightMin(), plant.getLightMax(), "lx")));
			} else if (above(reading.getLight(), plant.getLightMax())) {
				items.add(suggestion("SHADE", "차광", "광을 내립니다", plant, "광", formatLight(reading.getLight()), formatRange(plant.getLightMin(), plant.getLightMax(), "lx")));
			}
		}
		items.sort(Comparator.comparingInt(item -> deviceOrder(item.device())));
		return items;
	}

	private static ControlSuggestion suggestion(String device, String label, String effect, Plant plant, String metric, String value, String range) {
		return new ControlSuggestion(device, label, effect, plant.getName(), metric + " " + value + " · 기준 " + range);
	}

	private static boolean below(BigDecimal value, BigDecimal min) {
		return min != null && value.compareTo(min) < 0;
	}

	private static boolean above(BigDecimal value, BigDecimal max) {
		return max != null && value.compareTo(max) > 0;
	}

	private static boolean below(int value, Integer min) {
		return min != null && value < min;
	}

	private static boolean above(int value, Integer max) {
		return max != null && value > max;
	}

	private static int deviceOrder(String device) {
		return switch (device) {
			case "HEATER" -> 0;
			case "COOLER" -> 1;
			case "VENT" -> 2;
			case "HUMIDIFIER" -> 3;
			case "GROW_LIGHT" -> 4;
			default -> 5;
		};
	}

	private static ControlsResponse controlsOf(GreenhouseControl control) {
		if (control == null) {
			return new ControlsResponse(false, false, false, false, false, false);
		}
		return new ControlsResponse(
				control.isHeater(),
				control.isCooler(),
				control.isVent(),
				control.isHumidifier(),
				control.isGrowLight(),
				control.isShade());
	}

	private static GreenhouseControlState controlState(long greenhouseId, GreenhouseControl control) {
		ControlsResponse values = controlsOf(control);
		return new GreenhouseControlState(
				greenhouseId,
				values.heater(),
				values.cooler(),
				values.vent(),
				values.humidifier(),
				values.growLight(),
				values.shade());
	}

	@Transactional(readOnly = true)
	public List<BatchResponse> listBatches() {
		Map<Long, Long> quantities = quantitiesByBatch();
		return batches.findAllWithDetails().stream()
				.map(batch -> toResponse(batch, quantities.getOrDefault(batch.getId(), 0L)))
				.toList();
	}

	@Transactional(readOnly = true)
	public BatchDetailResponse batch(long id) {
		Batch batch = batches.findWithDetails(id)
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
				response.greenhouseId(),
				response.greenhouseName(),
				response.plantId(),
				response.plantName(),
				response.plantedOn(),
				response.stage(),
				response.note(),
				response.quantity(),
				history.stream().map(Stamp::item).toList());
	}

	@Transactional
	public long createBatch(long greenhouseId, long plantId, LocalDate plantedOn, Stage stage, int quantity, String note) {
		Greenhouse greenhouse = greenhouses.findById(greenhouseId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다."));
		Plant plant = plants.findById(plantId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "식물을 찾을 수 없습니다."));
		Stage initialStage = stage == null ? Stage.SEEDLING : stage;
		String trimmedNote = blankToNull(note);
		Batch batch = batches.save(new Batch(greenhouse, plant, plantedOn, initialStage, trimmedNote));
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

	private GreenhouseSummary summarizeGreenhouse(Greenhouse greenhouse, List<Batch> allBatches, Map<Long, Long> quantities) {
		long batchCount = 0;
		long total = 0;
		long sellable = 0;
		for (Batch batch : allBatches) {
			if (!batch.getGreenhouse().getId().equals(greenhouse.getId())) {
				continue;
			}
			long quantity = quantities.getOrDefault(batch.getId(), 0L);
			batchCount += 1;
			total += quantity;
			if (batch.getStage() == Stage.READY) {
				sellable += quantity;
			}
		}
		return new GreenhouseSummary(greenhouse.getId(), greenhouse.getName(), batchCount, total, sellable);
	}

	private PlantSummary summarizePlant(Plant plant, List<Batch> allBatches, Map<Long, Long> quantities) {
		long batchCount = 0;
		long total = 0;
		long sellable = 0;
		for (Batch batch : allBatches) {
			if (!batch.getPlant().getId().equals(plant.getId())) {
				continue;
			}
			long quantity = quantities.getOrDefault(batch.getId(), 0L);
			batchCount += 1;
			total += quantity;
			if (batch.getStage() == Stage.READY) {
				sellable += quantity;
			}
		}
		return new PlantSummary(
				plant.getId(),
				plant.getName(),
				plant.getDayTemp(),
				plant.getNightTemp(),
				plant.getLightNote(),
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
		return new BatchResponse(
				batch.getId(),
				batch.getGreenhouse().getId(),
				batch.getGreenhouse().getName(),
				batch.getPlant().getId(),
				batch.getPlant().getName(),
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
