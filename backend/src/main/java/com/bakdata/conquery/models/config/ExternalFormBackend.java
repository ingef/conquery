package com.bakdata.conquery.models.config;

import java.net.URL;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a generic form backend server.
 */
@CPSType(id = "EXTERNAL_FORM_BACKEND", base = PluginConfig.class)
@Getter
@Setter
public class ExternalFormBackend implements PluginConfig {

	@NotEmpty
	private String id;

	@NotNull
	private URL baseUrl;

	@NotNull
	private URL formConfigUrl;

	@NotNull
	private URL formPostUrl;

	@NotNull
	private URL statusUrl;


	@Override
	public void initialize(ManagerNode managerNode) {


	}
}