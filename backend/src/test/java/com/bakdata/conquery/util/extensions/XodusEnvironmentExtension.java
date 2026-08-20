package com.bakdata.conquery.util.extensions;

import java.io.File;
import java.nio.file.Files;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class XodusEnvironmentExtension implements Environment, BeforeAllCallback, AfterAllCallback {

	private final EnvironmentConfig config = new EnvironmentConfig();
	@Delegate
	private Environment environment;
	private File envPath;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		envPath = Files.createTempDirectory(getClass().getSimpleName()).toFile();
		envPath.deleteOnExit();
		environment = Environments.newInstance(envPath, config);

	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		environment.close();
		if(!envPath.delete()) {
			log.warn("Could not delete test directory: {}", envPath);
		}
	}
}
