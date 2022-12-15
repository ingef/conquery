package com.bakdata.conquery.apiv1.frontend;

import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;

/**
 * API Response for the dynamic configuration of the frontend
 *
 * @param version     backend version
 * @param currency    currency representation
 * @param queryUpload identifier specific column configuration for the query upload
 */
public record FEConfig(String version, FrontendConfig.CurrencyConfig currency, IdColumnConfig queryUpload) {
}
