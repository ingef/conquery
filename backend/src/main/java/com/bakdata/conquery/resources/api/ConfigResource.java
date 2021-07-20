package com.bakdata.conquery.resources.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import lombok.RequiredArgsConstructor;

@Path("config")
@Produces(AdditionalMediaTypes.JSON)

@RequiredArgsConstructor
public class ConfigResource {

	private final ConqueryConfig config;

	@GET
	@Path("frontend")
	public FrontendConfig getFrontendConfig() {


		return config.getFrontend()
					   .withQueryUpload(config.getFrontend().getQueryUpload().withIds(config.getIdMapping().getMappers()));
	}
}