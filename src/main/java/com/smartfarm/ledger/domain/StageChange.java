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
@Table(name = "stage_changes")
public class StageChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private Batch batch;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_stage", nullable = false, length = 16)
	private Stage fromStage;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_stage", nullable = false, length = 16)
	private Stage toStage;

	@Column(name = "changed_on", nullable = false)
	private LocalDate changedOn;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected StageChange() {
	}

	public StageChange(Batch batch, Stage fromStage, Stage toStage, LocalDate changedOn) {
		this.batch = batch;
		this.fromStage = fromStage;
		this.toStage = toStage;
		this.changedOn = changedOn;
	}

	public Long getId() {
		return id;
	}

	public Stage getFromStage() {
		return fromStage;
	}

	public Stage getToStage() {
		return toStage;
	}

	public LocalDate getChangedOn() {
		return changedOn;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
