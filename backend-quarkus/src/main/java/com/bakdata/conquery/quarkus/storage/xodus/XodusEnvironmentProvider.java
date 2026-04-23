package com.bakdata.conquery.quarkus.storage.xodus;

import java.nio.file.Path;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusEnvironmentProvider {

	@ConfigProperty(name = "conquery.storage.xodus.path", defaultValue = "storage/quarkus-meta")
	String xodusPath;

	private Environment environment;

	@PostConstruct
	void init() {
		environment = Environments.newInstance(Path.of(xodusPath).toFile());
	}

	@PreDestroy
	void close() {
		if (environment != null) {
			environment.close();
		}
	}

	public Environment getEnvironment() {
		return environment;
	}
}
