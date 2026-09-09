package com.bakdata.conquery.quarkus.storage.xodus;

import java.nio.file.Path;

import com.bakdata.conquery.quarkus.config.StorageRuntimeConfig;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;

@ApplicationScoped
@IfBuildProperty(name = "conquery.storage.backend", stringValue = "XODUS")
public class XodusEnvironmentProvider {

	@Inject
	StorageRuntimeConfig storageRuntimeConfig;

	@Getter
    private Environment environment;

	@PostConstruct
	void init() {
		environment = Environments.newInstance(Path.of(storageRuntimeConfig.xodus().path()).toFile());
	}

	@PreDestroy
	void close() {
		if (environment != null) {
			environment.close();
		}
	}

}
