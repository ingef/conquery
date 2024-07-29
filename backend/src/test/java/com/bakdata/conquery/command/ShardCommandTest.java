package com.bakdata.conquery.command;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.ShardCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ShardCommandTest {

	private static final ConqueryConfig CONQUERY_CONFIG = new ConqueryConfig() {{
		ConfigOverride.configureRandomPorts(this);
		this.setStorage(new NonPersistentStoreFactory());
	}};

	private static final DropwizardAppExtension<ConqueryConfig> EXT = new DropwizardAppExtension<>(
			Conquery.class,
			CONQUERY_CONFIG,
			application -> new ShardCommand()
	);

	@Test
	void checkHttpUp() {
		Client client = EXT.client();

		Response response = client.target(
						String.format("http://localhost:%d/metrics", EXT.getAdminPort()))
				.request()
				.get();

		assertThat(response.getStatus()).isEqualTo(200);
	}

}
