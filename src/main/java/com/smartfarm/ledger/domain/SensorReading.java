package com.smartfarm.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

	@Id
	@Column(name = "greenhouse_id")
	private Long greenhouseId;

	@Column(nullable = false, precision = 4, scale = 1)
	private BigDecimal temperature;

	@Column(nullable = false)
	private Integer humidity;

	@Column(nullable = false)
	private Integer light;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;

	protected SensorReading() {
	}

	public SensorReading(Long greenhouseId) {
		this.greenhouseId = greenhouseId;
	}

	public void record(BigDecimal temperature, int humidity, int light, LocalDateTime recordedAt) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.light = light;
		this.recordedAt = recordedAt;
	}

	public Long getGreenhouseId() {
		return greenhouseId;
	}

	public BigDecimal getTemperature() {
		return temperature;
	}

	public Integer getHumidity() {
		return humidity;
	}

	public Integer getLight() {
		return light;
	}

	public LocalDateTime getRecordedAt() {
		return recordedAt;
	}
}
