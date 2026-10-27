package com.smartfarm.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "plants")
public class Plant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String name;

	@Column(name = "day_temp", length = 40)
	private String dayTemp;

	@Column(name = "night_temp", length = 40)
	private String nightTemp;

	@Column(name = "light_note", length = 80)
	private String lightNote;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Plant() {
	}

	public Plant(String name, String dayTemp, String nightTemp, String lightNote) {
		this.name = name;
		this.dayTemp = dayTemp;
		this.nightTemp = nightTemp;
		this.lightNote = lightNote;
	}

	public void update(String name, String dayTemp, String nightTemp, String lightNote) {
		this.name = name;
		this.dayTemp = dayTemp;
		this.nightTemp = nightTemp;
		this.lightNote = lightNote;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDayTemp() {
		return dayTemp;
	}

	public String getNightTemp() {
		return nightTemp;
	}

	public String getLightNote() {
		return lightNote;
	}
}
