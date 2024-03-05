package com.bakdata.conquery.io.storage.xodus.stores;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.bakdata.conquery.models.config.XodusConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Slf4j
public class EnvironmentRegistry {

	@JsonIgnore
	private final BiMap<File, Environment> activeEnvironments = HashBiMap.create();

	public Environment register(File path, Environment environment) {

		final Environment proxyInstance = (Environment) Proxy.newProxyInstance(
				EnvironmentRegistry.class.getClassLoader(),
				new Class[]{Environment.class},
				new RegisteredEnvironment(environment)
		);

		synchronized (activeEnvironments) {
			activeEnvironments.put(path, proxyInstance);
		}
		return proxyInstance;
	}

	private void unregister(Environment environment) {
		log.debug("Unregister environment: {}", environment.getLocation());
		synchronized (activeEnvironments) {
			activeEnvironments.remove(activeEnvironments.inverse().get(environment));
		}
	}

	public Environment findEnvironment(@NonNull File path, XodusConfig xodusConfig) {
		synchronized (activeEnvironments) {

			try {
				return activeEnvironments.computeIfAbsent(path, newPath -> {
					final Environment environment = Environments.newInstance(newPath, xodusConfig.createConfig());
					return register(newPath, environment);
				});
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
					// Use proxy here because activeEnvironments holds the proxy
					unregister((Environment) proxy);
					return method.invoke(target, args);
				}
			}

			return method.invoke(target, args);
		}
	}
}
