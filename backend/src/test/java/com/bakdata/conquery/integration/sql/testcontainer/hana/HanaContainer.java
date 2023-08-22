package com.bakdata.conquery.integration.sql.testcontainer.hana;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Set;

import lombok.SneakyThrows;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class HanaContainer<SELF extends HanaContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

	public static final Integer DEFAULT_TENANT_HANA_PORT = 39041;
	private static final String MASTER_PASSWORD = "HXEHana1";
	private static final String USERNAME = "SYSTEM";
	private static final String DATABASE_NAME = "HXE";

	public HanaContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
		prepareTmpHanaDir();
		setWaitStrategy(
				new LogMessageWaitStrategy()
						.withRegEx(".*Startup finished.*\\s")
						.withStartupTimeout(Duration.ofMinutes(10))
		);
		withFileSystemBind("/tmp/data/hana", "/home/secrets");
		addExposedPort(DEFAULT_TENANT_HANA_PORT);
		setCommand(composeHanaArgs());
	}

	@Override
	public String getDriverClassName() {
		return "com.sap.cloud.db.jdbc";
	}

	@Override
	public String getJdbcUrl() {
		return "jdbc:sap://%s:%s/?databaseName=%s&encrypt=true&validateCertificate=false".formatted(
				getHost(),
				getMappedPort(DEFAULT_TENANT_HANA_PORT),
				DATABASE_NAME
		);
	}

	@Override
	public String getUsername() {
		return USERNAME;
	}

	@Override
	public String getPassword() {
		return MASTER_PASSWORD;
	}

	@Override
	public String getDatabaseName() {
		return DATABASE_NAME;
	}

	@Override
	protected String getTestQueryString() {
		return "SELECT 1";
	}

	@Override
	protected void waitUntilContainerStarted() {
		getWaitStrategy().waitUntilReady(this);
	}

	private String composeHanaArgs() {
		return "--agree-to-sap-license " +
			   "--passwords-url file:///home/secrets/password.json";
	}

	@SneakyThrows
	private static void prepareTmpHanaDir() {
		Path tmpHanaMountDir = Paths.get("/tmp/data/hana");
		Path masterPasswordFile = tmpHanaMountDir.resolve("password.json");
		String content = "{\"master_password\":\"%s\"}".formatted(MASTER_PASSWORD);

		Files.createDirectories(tmpHanaMountDir);
		Files.write(masterPasswordFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		Files.setPosixFilePermissions(tmpHanaMountDir, Set.of(PosixFilePermission.values()));
	}

}
