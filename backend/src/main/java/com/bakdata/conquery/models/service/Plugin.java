package com.bakdata.conquery.models.service;

import java.util.Optional;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.config.PluginConfig;

/**
 * TODO Doku
 */
public interface Plugin {

	/**
	 * If true, the plugin is registered, even if no config is found.
	 *
	 * @implNote It's probably more convenient to have a method {@code Optional<PluginConfig> getDefaultConfig()}
	 */
	boolean isDefault();

	Optional<Class<? extends PluginConfig>> getPluginConfigClass();

	/**
	 * Is called before {@link Plugin#initialize(ManagerNode)}
	 */
	void setConfig(PluginConfig config);

	default void initialize(ManagerNode managerNode) {
	}


}
