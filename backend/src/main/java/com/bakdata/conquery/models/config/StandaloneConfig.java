package com.bakdata.conquery.models.config;

import java.util.List;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StandaloneConfig {

	/**
	 * Port config for every shard in standalone mode.
	 *
	 * @implNote the application port is not used at the moment, so its port can be random
	 */
	private List<StandaloneShardConfig> shards = java.util.List.of(
			new StandaloneShardConfig(

					List.of(new HttpConnectorFactory() {{
						setPort(0);
					}}),
					List.of(new HttpConnectorFactory() {{
						setPort(8084);
					}})
			),
			new StandaloneShardConfig(
					List.of(new HttpConnectorFactory() {{
						setPort(0);
					}}),
					List.of(new HttpConnectorFactory() {{
						setPort(8086);
					}})
			)
	);

	@Data
	@AllArgsConstructor
	public static class StandaloneShardConfig {
		private List<ConnectorFactory> applicationConnectors;
		private List<ConnectorFactory> adminConnectors;

	}
}
