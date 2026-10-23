package com.smartfarm.ledger.web.dto;

import com.smartfarm.ledger.domain.MovementType;
import com.smartfarm.ledger.domain.Stage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class LedgerDtos {

	private LedgerDtos() {
	}

	public record CreateBatchRequest(
			@NotNull(message = "하우스를 선택해 주세요.") @Min(value = 1, message = "하우스를 선택해 주세요.") Long houseId,
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

	public record HouseSummary(
			long id,
			String code,
			String name,
			String cropCode,
			String cropName,
			String dayTemp,
			String nightTemp,
			String lightNote,
			long batchCount,
			long totalQuantity,
			long sellableQuantity) {
	}

	public record SummaryResponse(List<HouseSummary> houses, long totalQuantity, long sellableQuantity, long batchCount) {
	}

	public record BatchResponse(
			long id,
			long houseId,
			String houseName,
			String cropCode,
			String cropName,
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
			long houseId,
			String houseName,
			String cropCode,
			String cropName,
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
