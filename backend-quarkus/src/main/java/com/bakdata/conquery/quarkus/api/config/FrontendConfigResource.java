package com.bakdata.conquery.quarkus.api.config;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.quarkus.info.BuildInfo;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/config")
@Produces(MediaType.APPLICATION_JSON)
public class FrontendConfigResource {
	@Inject
	Instance<BuildInfo> buildInfo;

	@Inject
	FrontendRuntimeConfig frontendConfig;

	@GET
	@Path("/frontend")
	public FrontendConfigurationResponse getFrontendConfig() {

		FrontendRuntimeConfig.Currency currencyConfig = frontendConfig.currency();
		FrontendConfigurationResponse.CurrencyConfigResponse currency =
				new FrontendConfigurationResponse.CurrencyConfigResponse(
						currencyConfig.unit(),
						currencyConfig.thousandSeparator(),
						currencyConfig.decimalSeparator(),
						currencyConfig.decimalScale()
				);

		
		FrontendRuntimeConfig.QueryUpload queryUploadConfig = frontendConfig.queryUpload();
		List<FrontendConfigurationResponse.ColumnConfigResponse> configuredIds =
				queryUploadConfig.ids()
								 .orElse(List.of())
								 .stream()
								 .map(id -> new FrontendConfigurationResponse.ColumnConfigResponse(
										 id.name(),
										 id.field(),
										 Optional.ofNullable(id.label()).orElse(Map.of(Locale.ROOT.toString(), "result")),
										 id.primaryId(),
										 id.print()
								 ))
								 .toList();

		FrontendConfigurationResponse.IdColumnConfigResponse queryUpload =
				new FrontendConfigurationResponse.IdColumnConfigResponse(
						queryUploadConfig.table(),
						configuredIds.isEmpty()
								? List.of(
								new FrontendConfigurationResponse.ColumnConfigResponse(
										"ID",
										"pid",
										Map.of("und", "result"),
										true,
										true
								)
						)
								: configuredIds
				);

		List<FrontendConfigurationResponse.VersionContainerResponse> versions = List.of(
				new FrontendConfigurationResponse.VersionContainerResponse("Backend", getBackendVersion(), null)
		);

		return new FrontendConfigurationResponse(
				versions,
				currency,
				queryUpload,
				frontendConfig.manualUrl().orElse(null),
				frontendConfig.contactEmail().orElse(null),
				Year.now().minusYears(frontendConfig.observationPeriodYears()).atDay(1)
		);
	}

	private String getBackendVersion() {
		if (buildInfo != null && buildInfo.isResolvable()) {
			return buildInfo.get().version();
		}
		return "0.0.0-SNAPSHOT";
	}
}
