package com.bakdata.conquery.models.config.auth;

import javax.validation.constraints.NotEmpty;

/**
 * Allows a {@link com.bakdata.conquery.models.config.PluginConfig} to be defined multiple times with different ids.
 * Conquery validates the {@link com.bakdata.conquery.models.config.ConqueryConfig} to have only one instance of a {@link com.bakdata.conquery.models.config.PluginConfig} class to avoid clashes. However, if the class implements this interface, multiple instances are allowed. The implementations that require this type of plugin must check for the availability of the correct plugin by using the id.
 */
public interface MultiInstancePlugin {

	@NotEmpty
	String getId();
}
