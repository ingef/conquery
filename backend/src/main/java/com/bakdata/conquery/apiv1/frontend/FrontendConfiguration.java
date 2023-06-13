package com.bakdata.conquery.apiv1.frontend;

import java.net.URL;
import java.time.LocalDate;

import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;

/**
 * API Response for the dynamic configuration of the frontend
 *
 * @param version      backend version
 * @param currency     currency representation
 * @param queryUpload  identifier specific column configuration for the query upload
 * @param manualUrl    url to a user manual
 * @param contactEmail typical a mailto-url
 */
public record FrontendConfiguration(
		String version,
		FrontendConfig.CurrencyConfig currency,
		IdColumnConfig queryUpload,
		URL manualUrl,
		String contactEmail,
		LocalDate observationPeriodStart
) {
}
