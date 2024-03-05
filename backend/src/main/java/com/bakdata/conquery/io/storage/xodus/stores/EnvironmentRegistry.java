package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.config.XodusConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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

		final Environment proxyInstance = createProxy(environment);

		synchronized (activeEnvironments) {
			activeEnvironments.put(environment.getLocation(), proxyInstance);
		}
		return proxyInstance;
	}

	@NotNull
	private Environment createProxy(Environment environment) {
		return (Environment) Proxy.newProxyInstance(
				EnvironmentRegistry.class.getClassLoader(),
				new Class[]{Environment.class},
				new RegisteredEnvironment(environment)
		);
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
						newPath -> createProxy(Environments.newInstance(newPath, xodusConfig.createConfig()))
				);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to open environment: " + path, e);
			}
		}
	}

	@RequiredArgsConstructor
	private class RegisteredEnvironment implements InvocationHandler {

		private final Environment target;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if (method.getName().equals("close")) {
				log.debug("Environment was closed: {}", target.getLocation());
				synchronized (activeEnvironments) {
					// It does not matter if we use proxy or target for unregistering
					unregister((Environment) target);

					// Use target here to avoid endless loop
					return method.invoke(target, args);
				}
			}

			return method.invoke(target, args);
		}
	}
}
