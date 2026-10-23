package com.smartfarm.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "batches")
public class Batch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "house_id", nullable = false)
	private House house;

	@Column(name = "planted_on", nullable = false)
	private LocalDate plantedOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Stage stage;

	@Column(length = 200)
	private String note;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Batch() {
	}

	public Batch(House house, LocalDate plantedOn, Stage stage, String note) {
		this.house = house;
		this.plantedOn = plantedOn;
		this.stage = stage;
		this.note = note;
	}

	public void changeStage(Stage stage) {
		this.stage = stage;
	}

	public Long getId() {
		return id;
	}

	public House getHouse() {
		return house;
	}

	public LocalDate getPlantedOn() {
		return plantedOn;
	}

	public Stage getStage() {
		return stage;
	}

	public String getNote() {
		return note;
	}
}
