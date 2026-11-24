package com.bakdata.conquery.apiv1.frontend;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.config.IdColumnConfig;

/**
 * API Response for the dynamic configuration of the frontend
 *
 * @param versions            mapping of backend and form backend ids to their versions (version can be null)
 * @param currency            ISO 4217 code of the currency that money columns hold
 * @param queryUpload         identifier specific column configuration for the query upload
 * @param manualUrl           url to a user manual
 * @param contactEmail        typical a mailto-url
 */
public record FrontendConfiguration(
		List<VersionContainer> versions,
		String currency,
		IdColumnConfig queryUpload,
		URL manualUrl,
		String contactEmail,
		LocalDate observationPeriodStart
) {
}
