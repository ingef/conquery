package com.bakdata.conquery.resources.api;

import java.time.Year;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.frontend.FrontendConfiguration;
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

		final IdColumnConfig idColumns = config.getIdColumns().withIds(config.getIdColumns().getIds());
		final FrontendConfig frontendConfig = config.getFrontend();

		return new FrontendConfiguration(
				VersionInfo.INSTANCE.getVersions(),
				frontendConfig.getCurrency(),
				idColumns,
				frontendConfig.getManualUrl(),
				frontendConfig.getContactEmail(),
				Year.now().minusYears(frontendConfig.getObservationPeriodYears()).atDay(1)
		);
	}

}
