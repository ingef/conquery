package com.bakdata.conquery.models.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.google.common.collect.MoreCollectors;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO Doku
 */
@Slf4j
public class Plugins {

	@Setter(AccessLevel.PRIVATE)
	private List<Plugin> plugins;


	public void registerPlugins(ConqueryConfig config) {
		List<Plugin> plugins = new ArrayList<>();

		ServiceLoader<Plugin> pluginServiceLoader = ServiceLoader.load(Plugin.class);
		for (Plugin plugin : pluginServiceLoader) {
			final Class<? extends PluginConfig> pluginConfigClass = plugin.getPluginConfigClass();

			final Optional<? extends PluginConfig> pluginConfig = config.getPluginConfig(pluginConfigClass);
			if (pluginConfig.isEmpty()) {
				log.trace("Discovered plugin '{}' has no configuration provided", plugin);
				if (!plugin.isDefault()) {
					log.info("Skipping registration of plugin '{}'. It is neither default nor was a configuration provided", plugin);
					continue;
				}
				// Default plugins must come with their defaults configured, so we can just add the plugin
			}
			else {
				// Add provided configuration to plugin
				final PluginConfig userPluginConfig = pluginConfig.get();
				plugin.setConfig(userPluginConfig);
				log.info("Registering plugin '{}' with provided config", plugin);

			}
			plugins.add(plugin);
		}

		setPlugins(plugins);
	}

	public void initializePlugins(ManagerNode managerNode) {
		plugins.forEach(p -> p.initialize(managerNode));
	}

	public <T> Optional<T> getPlugin(Class<T> clazz) {
		return plugins.stream().filter(clazz::isInstance).map(clazz::cast).collect(MoreCollectors.toOptional());
	}

	/**
	 * Might cache these
	 *
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public <T> Stream<T> getPlugins(Class<T> clazz) {
		return plugins.stream().filter(clazz::isInstance).map(clazz::cast);
	}
}
