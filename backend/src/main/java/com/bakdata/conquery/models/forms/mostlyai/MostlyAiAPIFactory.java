package com.bakdata.conquery.models.forms.mostlyai;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.PluginConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.client.JerseyClientConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

@CPSType(id = "MOSTLY_AI", base = PluginConfig.class)
@Getter
@Setter
public class MostlyAiAPIFactory implements PluginConfig {

	@NotNull
	private URI baseUri;

	@NotEmpty
	private String apiKey;


	@Valid
	@NotNull
	private ClientConfig clientConfig = new ClientConfig();

	@JsonIgnore
	@Setter(AccessLevel.PRIVATE)
	private Client client;

	@Override
	public void initialize(ManagerNode managerNode) {

		/*
		Because of this old bug https://groups.google.com/g/dropwizard-user/c/WeLt4J_dIqs we cannot use the io.dropwizard.client.JerseyClientBuilder
		 */
		client = JerseyClientBuilder.createClient(clientConfig);
	}

	public MostlyAiApi createAPI() {
		return new MostlyAiApi(client, baseUri, apiKey);
	}
}
