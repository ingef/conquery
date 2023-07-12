package com.bakdata.conquery.resources.api;

import java.time.Year;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.frontend.FrontendConfiguration;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.util.VersionInfo;
import lombok.RequiredArgsConstructor;

@Path("config")
@Produces(AdditionalMediaTypes.JSON)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigResource {

	private final ConqueryConfig config;

	@GET
	@Path("frontend")
	public FrontendConfiguration getFrontendConfig() {
		// Filter Ids that are not resolvable
		final IdColumnConfig idColumns = config.getIdColumns().withIds(config.getIdColumns()
																			 .getIds()
																			 .stream()
																			 .filter(ColumnConfig::isResolvable)
																			 .toList());

		final FrontendConfig frontendConfig = config.getFrontend();
		return new FrontendConfiguration(
				VersionInfo.INSTANCE.getProjectVersion(),
				frontendConfig.getCurrency(),
				idColumns,
				frontendConfig.getManualUrl(),
				frontendConfig.getContactEmail(),
				Year.now().minusYears(frontendConfig.getObservationPeriodYears()).atDay(1)
		);
	}

}