package com.smartfarm.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "greenhouse_controls")
public class GreenhouseControl {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Id
	@Column(name = "greenhouse_id")
	private Long greenhouseId;

	@Column(nullable = false)
	private boolean heater;

	@Column(nullable = false)
	private boolean cooler;

	@Column(nullable = false)
	private boolean vent;

	@Column(nullable = false)
	private boolean humidifier;

	@Column(name = "grow_light", nullable = false)
	private boolean growLight;

	@Column(nullable = false)
	private boolean shade;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected GreenhouseControl() {
	}

	public GreenhouseControl(Long greenhouseId) {
		this.greenhouseId = greenhouseId;
		this.updatedAt = LocalDateTime.now(SEOUL);
	}

	public void update(boolean heater, boolean cooler, boolean vent, boolean humidifier, boolean growLight, boolean shade) {
		this.heater = heater;
		this.cooler = cooler;
		this.vent = vent;
		this.humidifier = humidifier;
		this.growLight = growLight;
		this.shade = shade;
		this.updatedAt = LocalDateTime.now(SEOUL);
	}

	public Long getGreenhouseId() {
		return greenhouseId;
	}

	public boolean isHeater() {
		return heater;
	}

	public boolean isCooler() {
		return cooler;
	}

	public boolean isVent() {
		return vent;
	}

	public boolean isHumidifier() {
		return humidifier;
	}

	public boolean isGrowLight() {
		return growLight;
	}

	public boolean isShade() {
		return shade;
	}
}
