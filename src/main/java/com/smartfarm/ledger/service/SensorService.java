package com.smartfarm.ledger.service;

import com.smartfarm.ledger.domain.Greenhouse;
import com.smartfarm.ledger.domain.SensorReading;
import com.smartfarm.ledger.repo.GreenhouseRepository;
import com.smartfarm.ledger.repo.SensorReadingRepository;
import com.smartfarm.ledger.web.dto.LedgerDtos.SensorReadingRequest;
import com.smartfarm.ledger.web.dto.LedgerDtos.SensorReadingResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SensorService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final GreenhouseRepository greenhouses;
	private final SensorReadingRepository readings;
	private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();

	public SensorService(GreenhouseRepository greenhouses, SensorReadingRepository readings) {
		this.greenhouses = greenhouses;
		this.readings = readings;
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(0L);
		clients.add(emitter);
		emitter.onCompletion(() -> clients.remove(emitter));
		emitter.onTimeout(() -> clients.remove(emitter));
		emitter.onError(error -> clients.remove(emitter));
		publish(latest(), emitter);
		return emitter;
	}

	@Transactional(readOnly = true)
	public List<SensorReadingResponse> latest() {
		Map<Long, Greenhouse> houses = greenhouses.findAll().stream()
				.collect(Collectors.toMap(Greenhouse::getId, Function.identity()));
		return readings.findAll().stream()
				.filter(reading -> houses.containsKey(reading.getGreenhouseId()))
				.sorted(Comparator.comparing(SensorReading::getGreenhouseId))
				.map(reading -> new SensorReadingResponse(
						reading.getGreenhouseId(),
						houses.get(reading.getGreenhouseId()).getName(),
						reading.getTemperature(),
						reading.getHumidity(),
						reading.getLight(),
						reading.getRecordedAt()))
				.toList();
	}

	@Transactional
	public void record(List<SensorReadingRequest> requests) {
		LocalDateTime now = LocalDateTime.now(SEOUL);
		for (SensorReadingRequest request : requests) {
			if (!greenhouses.existsById(request.greenhouseId())) {
				throw new ApiException(HttpStatus.NOT_FOUND, "온실을 찾을 수 없습니다.");
			}
			SensorReading reading = readings.findById(request.greenhouseId())
					.orElseGet(() -> new SensorReading(request.greenhouseId()));
			reading.record(request.temperature().setScale(1, RoundingMode.HALF_UP), request.humidity(), request.light(), now);
			readings.save(reading);
		}
		List<SensorReadingResponse> snapshot = latest();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish(snapshot);
			}
		});
	}

	@Scheduled(fixedRate = 15000)
	void keepAlive() {
		for (SseEmitter client : clients) {
			try {
				client.send(SseEmitter.event().comment("ping"));
			} catch (Exception ex) {
				clients.remove(client);
				client.complete();
			}
		}
	}

	private void publish(List<SensorReadingResponse> snapshot) {
		for (SseEmitter client : clients) {
			publish(snapshot, client);
		}
	}

	private void publish(List<SensorReadingResponse> snapshot, SseEmitter client) {
		try {
			client.send(SseEmitter.event().name("readings").data(snapshot, MediaType.APPLICATION_JSON));
		} catch (Exception ex) {
			clients.remove(client);
			client.complete();
		}
	}
}
