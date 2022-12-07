package com.bakdata.conquery.models.forms.mostlyai;

import java.net.URI;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.external.ExternalResultProvider;
import com.bakdata.conquery.models.config.PluginConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.client.JerseyClientBuilder;

@CPSType(id = "MOSTLY_AI", base = PluginConfig.class)
@Getter
@Setter
public class MostlyAiAPIFactory implements PluginConfig {

	@NotNull
	private URI baseUri;

	@NotEmpty
	private String apiKey;

	@JsonIgnore
	@Setter(AccessLevel.PRIVATE)
	private Client client;

	@Override
	public void initialize(ManagerNode managerNode) {


		// Check that an ExternalResultProvider is configured
		managerNode.getConfig().getResultProviders()
				   .stream()
				   .filter(ExternalResultProvider.class::isInstance)
				   .collect(MoreCollectors.toOptional())
				   .orElseThrow(() -> new IllegalStateException("This execution requires an ExternalResultProvider to be configured."));

		/*
		Because of this old bug https://groups.google.com/g/dropwizard-user/c/WeLt4J_dIqs we cannot use the io.dropwizard.client.JerseyClientBuilder
		 */
		client = JerseyClientBuilder.createClient();
	}

	public MostlyAiApi createAPI() {
		return new MostlyAiApi(client, baseUri, apiKey);
	}
}
