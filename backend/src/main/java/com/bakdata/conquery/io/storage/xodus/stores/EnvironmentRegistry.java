package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.config.XodusConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps transparently track of open environments using a map.
 * If an environment is closed it is automatically unregistered.
 */
@RequiredArgsConstructor
@Slf4j
public class EnvironmentRegistry {

	@JsonIgnore
	private final Map<String, Environment> activeEnvironments = new HashMap<>();

	public Environment register(Environment environment) {

		final Environment proxyInstance = createManaged(environment);

		synchronized (activeEnvironments) {
			activeEnvironments.put(environment.getLocation(), proxyInstance);
		}
		return proxyInstance;
	}

	@NotNull
	private Environment createManaged(Environment environment) {
		return new ManagedEnvironment(environment);
	}

	private void unregister(Environment environment) {
		log.debug("Unregister environment: {}", environment.getLocation());
		synchronized (activeEnvironments) {
			final Environment remove = activeEnvironments.remove(environment.getLocation());

			if (remove == null) {
				log.warn("Could not unregister environment, because it was not registered: {}", environment.getLocation());
			}
		}
	}

	public Environment findOrCreateEnvironment(@NonNull File path, XodusConfig xodusConfig) {
		synchronized (activeEnvironments) {

			try {
				// Check for old env or register new env
				return activeEnvironments.computeIfAbsent(
						path.toString(),
						newPath -> createManaged(Environments.newInstance(newPath, xodusConfig.createConfig()))
				);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to open environment: " + path, e);
			}
		}
	}

	@RequiredArgsConstructor
	public class ManagedEnvironment implements Environment {

		@Delegate
		private final Environment delegate;

		public void close() {
			synchronized (activeEnvironments) {
				log.debug("Environment was closed: {}", delegate.getLocation());
				unregister(delegate);
				delegate.close();
			}
		}
	}
}
