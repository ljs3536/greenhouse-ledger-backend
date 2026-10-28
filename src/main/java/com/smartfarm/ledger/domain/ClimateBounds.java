package com.smartfarm.ledger.domain;

import java.math.BigDecimal;

public record ClimateBounds(
		BigDecimal dayTempMin,
		BigDecimal dayTempMax,
		BigDecimal nightTempMin,
		BigDecimal nightTempMax,
		Integer humidityMin,
		Integer humidityMax,
		Integer lightMin,
		Integer lightMax) {
}
