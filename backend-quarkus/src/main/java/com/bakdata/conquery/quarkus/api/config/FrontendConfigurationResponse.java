package com.bakdata.conquery.quarkus.api.config;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record FrontendConfigurationResponse(
		List<VersionContainerResponse> versions,
		CurrencyConfigResponse currency,
		IdColumnConfigResponse queryUpload,
		String manualUrl,
		String contactEmail,
		LocalDate observationPeriodStart
) {

	public record VersionContainerResponse(
			String name,
			String version,
			String error
	) {
	}

	public record CurrencyConfigResponse(
			String unit,
			String thousandSeparator,
			String decimalSeparator,
			int decimalScale
	) {
	}

	public record IdColumnConfigResponse(
			String table,
			List<ColumnConfigResponse> ids
	) {
	}

	public record ColumnConfigResponse(
			String name,
			String field,
			Map<String, String> label,
			boolean primaryId,
			boolean print
	) {
	}
}
