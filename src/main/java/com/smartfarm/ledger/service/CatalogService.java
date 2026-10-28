package com.smartfarm.ledger.service;

import com.smartfarm.ledger.domain.Batch;
import com.smartfarm.ledger.domain.ClimateBounds;
import com.smartfarm.ledger.domain.Greenhouse;
import com.smartfarm.ledger.domain.Plant;
import com.smartfarm.ledger.repo.BatchRepository;
import com.smartfarm.ledger.repo.GreenhouseRepository;
import com.smartfarm.ledger.repo.PlantRepository;
import com.smartfarm.ledger.web.dto.LedgerDtos.GreenhouseResponse;
import com.smartfarm.ledger.web.dto.LedgerDtos.PlantResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

	private final GreenhouseRepository greenhouses;
	private final PlantRepository plants;
	private final BatchRepository batches;

	public CatalogService(GreenhouseRepository greenhouses, PlantRepository plants, BatchRepository batches) {
		this.greenhouses = greenhouses;
		this.plants = plants;
		this.batches = batches;
	}

	@Transactional(readOnly = true)
	public List<GreenhouseResponse> listGreenhouses() {
		Map<Long, Long> counts = batchCounts(true);
		return greenhouses.findAll().stream()
				.sorted(Comparator.comparing(Greenhouse::getId))
				.map(greenhouse -> new GreenhouseResponse(
						greenhouse.getId(),
						greenhouse.getName(),
						counts.getOrDefault(greenhouse.getId(), 0L)))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PlantResponse> listPlants() {
		Map<Long, Long> counts = batchCounts(false);
		return plants.findAll().stream()
				.sorted(Comparator.comparing(Plant::getName))
				.map(plant -> new PlantResponse(
						plant.getId(),
						plant.getName(),
						plant.getDayTemp(),
						plant.getNightTemp(),
						plant.getLightNote(),
						plant.getSeedlingDays(),
						plant.getGrowthDays(),
						plant.getSpikeDays(),
						plant.getBloomDays(),
						plant.getDayTempMin(),
						plant.getDayTempMax(),
						plant.getNightTempMin(),
						plant.getNightTempMax(),
						plant.getHumidityMin(),
						plant.getHumidityMax(),
						plant.getLightMin(),
						plant.getLightMax(),
						counts.getOrDefault(plant.getId(), 0L)))
				.toList();
	}

	@Transactional
	public long createGreenhouse(String name) {
		String trimmed = requireName(name, "온실 이름을 입력해 주세요.");
		ensureGreenhouseNameFree(trimmed, null);
		return greenhouses.save(new Greenhouse(trimmed)).getId();
	}

	@Transactional
	public void updateGreenhouse(long id, String name) {
		Greenhouse greenhouse = greenhouses.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다."));
		String trimmed = requireName(name, "온실 이름을 입력해 주세요.");
		ensureGreenhouseNameFree(trimmed, id);
		greenhouse.rename(trimmed);
	}

	@Transactional
	public void deleteGreenhouse(long id) {
		if (!greenhouses.existsById(id)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다.");
		}
		if (batches.existsByGreenhouse_Id(id)) {
			throw new ApiException(HttpStatus.CONFLICT, "이 온실에 배치가 있어 삭제할 수 없습니다. 배치를 먼저 정리해 주세요.");
		}
		greenhouses.deleteById(id);
	}

	@Transactional
	public long createPlant(String name, String dayTemp, String nightTemp, String lightNote, Integer seedlingDays, Integer growthDays, Integer spikeDays, Integer bloomDays, ClimateBounds bounds) {
		String trimmed = requireName(name, "식물 이름을 입력해 주세요.");
		ensurePlantNameFree(trimmed, null);
		ClimateBounds checked = checkBounds(bounds);
		return plants.save(new Plant(
				trimmed,
				blankToNull(dayTemp),
				blankToNull(nightTemp),
				blankToNull(lightNote),
				seedlingDays,
				growthDays,
				spikeDays,
				bloomDays,
				checked)).getId();
	}

	@Transactional
	public void updatePlant(long id, String name, String dayTemp, String nightTemp, String lightNote, Integer seedlingDays, Integer growthDays, Integer spikeDays, Integer bloomDays, ClimateBounds bounds) {
		Plant plant = plants.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "식물을 찾을 수 없습니다."));
		String trimmed = requireName(name, "식물 이름을 입력해 주세요.");
		ensurePlantNameFree(trimmed, id);
		plant.update(trimmed, blankToNull(dayTemp), blankToNull(nightTemp), blankToNull(lightNote), seedlingDays, growthDays, spikeDays, bloomDays, checkBounds(bounds));
	}

	@Transactional
	public void deletePlant(long id) {
		if (!plants.existsById(id)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "식물을 찾을 수 없습니다.");
		}
		if (batches.existsByPlant_Id(id)) {
			throw new ApiException(HttpStatus.CONFLICT, "이 식물로 등록된 배치가 있어 삭제할 수 없습니다. 배치를 먼저 정리해 주세요.");
		}
		plants.deleteById(id);
	}

	private Map<Long, Long> batchCounts(boolean greenhouse) {
		Map<Long, Long> counts = new HashMap<>();
		for (Batch batch : batches.findAllWithDetails()) {
			long id = greenhouse ? batch.getGreenhouse().getId() : batch.getPlant().getId();
			counts.merge(id, 1L, Long::sum);
		}
		return counts;
	}

	private void ensureGreenhouseNameFree(String name, Long currentId) {
		greenhouses.findByName(name).ifPresent(existing -> {
			if (currentId == null || !existing.getId().equals(currentId)) {
				throw new ApiException(HttpStatus.CONFLICT, "같은 이름의 온실이 있습니다.");
			}
		});
	}

	private ClimateBounds checkBounds(ClimateBounds bounds) {
		requireOrder("낮 온도", bounds.dayTempMin(), bounds.dayTempMax());
		requireOrder("밤 온도", bounds.nightTempMin(), bounds.nightTempMax());
		requireOrder("습도", bounds.humidityMin(), bounds.humidityMax());
		requireOrder("광량", bounds.lightMin(), bounds.lightMax());
		return new ClimateBounds(
				scale(bounds.dayTempMin()),
				scale(bounds.dayTempMax()),
				scale(bounds.nightTempMin()),
				scale(bounds.nightTempMax()),
				bounds.humidityMin(),
				bounds.humidityMax(),
				bounds.lightMin(),
				bounds.lightMax());
	}

	private void requireOrder(String label, BigDecimal min, BigDecimal max) {
		if (min != null && max != null && min.compareTo(max) > 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, label + " 최소값이 최댓값보다 큽니다.");
		}
	}

	private void requireOrder(String label, Integer min, Integer max) {
		if (min != null && max != null && min > max) {
			throw new ApiException(HttpStatus.BAD_REQUEST, label + " 최소값이 최댓값보다 큽니다.");
		}
	}

	private BigDecimal scale(BigDecimal value) {
		return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
	}

	private void ensurePlantNameFree(String name, Long currentId) {
		plants.findByName(name).ifPresent(existing -> {
			if (currentId == null || !existing.getId().equals(currentId)) {
				throw new ApiException(HttpStatus.CONFLICT, "같은 이름의 식물이 있습니다.");
			}
		});
	}

	private static String requireName(String value, String message) {
		String trimmed = blankToNull(value);
		if (trimmed == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, message);
		}
		return trimmed;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
