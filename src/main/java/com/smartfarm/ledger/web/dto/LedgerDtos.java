package com.smartfarm.ledger.web.dto;

import com.smartfarm.ledger.domain.MovementType;
import com.smartfarm.ledger.domain.Stage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class LedgerDtos {

	private LedgerDtos() {
	}

	public record CreateGreenhouseRequest(
			@NotBlank(message = "온실 이름을 입력해 주세요.") @Size(max = 40, message = "이름은 40자까지 입력할 수 있습니다.") String name) {
	}

	public record CreatePlantRequest(
			@NotBlank(message = "식물 이름을 입력해 주세요.") @Size(max = 40, message = "이름은 40자까지 입력할 수 있습니다.") String name,
			@Size(max = 40, message = "낮 온도는 40자까지 입력할 수 있습니다.") String dayTemp,
			@Size(max = 40, message = "밤 온도는 40자까지 입력할 수 있습니다.") String nightTemp,
			@Size(max = 80, message = "광 메모는 80자까지 입력할 수 있습니다.") String lightNote,
			@Min(value = 1, message = "기준 일수는 1 이상의 정수입니다.") @Max(value = 3650, message = "기준 일수는 3650일까지 적을 수 있습니다.") Integer seedlingDays,
			@Min(value = 1, message = "기준 일수는 1 이상의 정수입니다.") @Max(value = 3650, message = "기준 일수는 3650일까지 적을 수 있습니다.") Integer growthDays,
			@Min(value = 1, message = "기준 일수는 1 이상의 정수입니다.") @Max(value = 3650, message = "기준 일수는 3650일까지 적을 수 있습니다.") Integer spikeDays,
			@Min(value = 1, message = "기준 일수는 1 이상의 정수입니다.") @Max(value = 3650, message = "기준 일수는 3650일까지 적을 수 있습니다.") Integer bloomDays) {
	}

	public record CreateBatchRequest(
			@NotNull(message = "온실을 선택해 주세요.") @Min(value = 1, message = "온실을 선택해 주세요.") Long greenhouseId,
			@NotNull(message = "식물을 선택해 주세요.") @Min(value = 1, message = "식물을 선택해 주세요.") Long plantId,
			@NotNull(message = "심은 날 형식이 올바르지 않습니다.") LocalDate plantedOn,
			Stage stage,
			@NotNull(message = "수량은 1 이상의 정수입니다.") @Min(value = 1, message = "수량은 1 이상의 정수입니다.") @Max(value = 100_000, message = "수량은 1 이상의 정수입니다.") Integer quantity,
			@Size(max = 200, message = "글자는 200자까지 남길 수 있습니다.") String note) {
	}

	public record StageRequest(@NotNull(message = "생장 단계를 확인해 주세요.") Stage stage) {
	}

	public record MovementRequest(
			@NotNull(message = "입고, 폐기, 판매 중 하나를 선택해 주세요.") MovementType type,
			@NotNull(message = "수량은 1 이상의 정수입니다.") @Min(value = 1, message = "수량은 1 이상의 정수입니다.") @Max(value = 100_000, message = "수량은 1 이상의 정수입니다.") Integer quantity,
			@NotNull(message = "기록 날짜 형식이 올바르지 않습니다.") LocalDate occurredOn,
			@Size(max = 200, message = "글자는 200자까지 남길 수 있습니다.") String memo) {
	}

	public record GreenhouseResponse(long id, String name, long batchCount) {
	}

	public record PlantResponse(
			long id,
			String name,
			String dayTemp,
			String nightTemp,
			String lightNote,
			Integer seedlingDays,
			Integer growthDays,
			Integer spikeDays,
			Integer bloomDays,
			long batchCount) {
	}

	public record AttentionItem(
			long batchId,
			long greenhouseId,
			String greenhouseName,
			String plantName,
			Stage stage,
			long quantity,
			int daysInStage,
			Integer referenceDays) {
	}

	public record GreenhouseSummary(long id, String name, long batchCount, long totalQuantity, long sellableQuantity) {
	}

	public record PlantSummary(
			long id,
			String name,
			String dayTemp,
			String nightTemp,
			String lightNote,
			long batchCount,
			long totalQuantity,
			long sellableQuantity) {
	}

	public record StageQuantity(Stage stage, long quantity) {
	}

	public record SummaryResponse(
			List<AttentionItem> attention,
			List<GreenhouseSummary> greenhouses,
			List<PlantSummary> plants,
			List<StageQuantity> stages,
			long totalQuantity,
			long sellableQuantity,
			long batchCount) {
	}

	public record BatchResponse(
			long id,
			long greenhouseId,
			String greenhouseName,
			long plantId,
			String plantName,
			LocalDate plantedOn,
			Stage stage,
			String note,
			long quantity) {
	}

	public record HistoryItem(
			String id,
			String kind,
			LocalDate date,
			String memo,
			MovementType type,
			Integer quantity,
			Stage fromStage,
			Stage toStage) {
	}

	public record BatchDetailResponse(
			long id,
			long greenhouseId,
			String greenhouseName,
			long plantId,
			String plantName,
			LocalDate plantedOn,
			Stage stage,
			String note,
			long quantity,
			List<HistoryItem> history) {
	}

	public record IdResponse(long id) {
	}

	public record OkResponse(boolean ok) {
	}
}
