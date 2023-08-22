package com.bakdata.conquery.integration.sql.testcontainer.hana;

import java.time.Duration;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class HanaContainer<SELF extends HanaContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

	public static final Integer DEFAULT_TENANT_HANA_PORT = 39041;
	public static final String DEFAULT_MASTER_PASSWORD = "HXEHana1";
	private static final String USERNAME = "SYSTEM";
	private static final String DATABASE_NAME = "HXE";

	public HanaContainer(DockerImageName dockerImageName) {
		super(dockerImageName);
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
		return DEFAULT_MASTER_PASSWORD;
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

}
