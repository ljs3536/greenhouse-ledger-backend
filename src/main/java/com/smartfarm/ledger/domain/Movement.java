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
@Table(name = "movements")
public class Movement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private Batch batch;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MovementType type;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "occurred_on", nullable = false)
	private LocalDate occurredOn;

	@Column(length = 200)
	private String memo;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Movement() {
	}

	public Movement(Batch batch, MovementType type, int quantity, LocalDate occurredOn, String memo) {
		this.batch = batch;
		this.type = type;
		this.quantity = quantity;
		this.occurredOn = occurredOn;
		this.memo = memo;
	}

	public Long getId() {
		return id;
	}

	public MovementType getType() {
		return type;
	}

	public int getQuantity() {
		return quantity;
	}

	public LocalDate getOccurredOn() {
		return occurredOn;
	}

	public String getMemo() {
		return memo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
