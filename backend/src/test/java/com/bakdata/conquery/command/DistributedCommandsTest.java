package com.bakdata.conquery.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.ShardCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.ClusterHealthCheck;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.ConfigOverride;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.util.Duration;
import lombok.Data;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DistributedCommandsTest {

	public static final Duration CONNECT_RETRY_TIMEOUT = Duration.seconds(1);

	private static final ConqueryConfig CONQUERY_CONFIG_MANAGER = new ConqueryConfig() {{
		ConfigOverride.configureRandomPorts(this);
		this.setStorage(new NonPersistentStoreFactory());
	}};

	private static final ConqueryConfig CONQUERY_CONFIG_SHARD = new ConqueryConfig() {{
		ConfigOverride.configureRandomPorts(this);
		this.getCluster().setPort(CONQUERY_CONFIG_MANAGER.getCluster().getPort());
		this.getCluster().setConnectRetryTimeout(CONNECT_RETRY_TIMEOUT);
		this.setStorage(new NonPersistentStoreFactory());
	}};

	private static final DropwizardAppExtension<ConqueryConfig> MANAGER = new DropwizardAppExtension<>(
			Conquery.class,
			CONQUERY_CONFIG_MANAGER,
			ServerCommand::new
	);

	private static final DropwizardAppExtension<ConqueryConfig> SHARD = new DropwizardAppExtension<>(
			Conquery.class,
			CONQUERY_CONFIG_SHARD,
			application -> new ShardCommand()
	);

	@Test
	@Order(0)
	void checkHttpUpShard() {
		Client client = SHARD.client();

		Response response = client.target(
										  String.format("http://localhost:%d/ping", SHARD.getAdminPort()))
				.request()
				.get();

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	@Order(0)
	void checkHttpUpManager() {
		Client client = MANAGER.client();

		Response response = client.target(
										  String.format("http://localhost:%d/ping", MANAGER.getAdminPort()))
								  .request()
								  .get();

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	@Order(1)
	void clusterEstablished() throws InterruptedException {
		Client client = MANAGER.client();

		// Wait for Shard to be connected
		// May use https://github.com/awaitility/awaitility here in the future
		Thread.sleep(2 * CONNECT_RETRY_TIMEOUT.toMilliseconds());

		Response response = client.target(
										  String.format("http://localhost:%d/healthcheck", MANAGER.getAdminPort()))
								  .request()
								  .get();


		assertThat(response.getStatus()).isEqualTo(200);

		Map<String, GenericHealthCheckResult> healthCheck = response.readEntity(new GenericType<>() {
		});

		assertThat(healthCheck).containsKey("cluster");
		assertThat(healthCheck.get("cluster").healthy).isTrue();
		assertThat(healthCheck.get("cluster").getMessage()).isEqualTo(String.format(ClusterHealthCheck.HEALTHY_MESSAGE_FMT, 1));
	}

	@Test
	@Order(2)
	void shardReconnect() throws Exception {
		Client client = MANAGER.client();

		// Shutdown shard
		SHARD.after();

		Thread.sleep(2 * CONNECT_RETRY_TIMEOUT.toMilliseconds());


		// Check cluster is unhealthy
		Response response = client.target(
										  String.format("http://localhost:%d/healthcheck", MANAGER.getAdminPort()))
								  .request()
								  .get();

		assertThat(response.getStatus()).isEqualTo(500);

		Map<String, GenericHealthCheckResult> healthCheck = response.readEntity(new GenericType<>() {
		});

		assertThat(healthCheck).containsKey("cluster");
		assertThat(healthCheck.get("cluster").healthy).isFalse();

		// Restart shard
		SHARD.before();


		response = client.target(
								 String.format("http://localhost:%d/healthcheck", MANAGER.getAdminPort()))
						 .request()
						 .get();

		assertThat(response.getStatus()).isEqualTo(200);

		healthCheck = response.readEntity(new GenericType<>() {
		});

		assertThat(healthCheck).containsKey("cluster");
		assertThat(healthCheck.get("cluster").healthy).isTrue();
	}

	/**
	 * Just a container to parse necessary information as {@link com.codahale.metrics.health.HealthCheck.Result}
	 * gives no useful binding to Jackson (missing getters and constructors, private members).
	 */
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class GenericHealthCheckResult {
		private boolean healthy;
		private String message;
	}

}
