package com.bakdata.conquery.models.config;

import java.net.InetAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.dropwizard.core.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.PortRange;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClusterConfig extends Configuration {
	@PortRange
	private int port = 16170;
	@Valid
	@NotNull
	private InetAddress managerURL = InetAddress.getLoopbackAddress();
	@Valid
	@NotNull
	private MinaConfig mina = new MinaConfig();
	@Min(1)
	private int entityBucketSize = 1000;

	private Duration idleTimeOut =  Duration.minutes(5);
	private Duration connectRetryTimeout = Duration.seconds(30);

	/**
	 * Amount of backpressure before jobs can volunteer to block to send messages to their shards.
	 * <p>
	 * Mostly {@link com.bakdata.conquery.models.jobs.ImportJob} is interested in this. Note that an average import should create more than #Entities / {@linkplain #entityBucketSize} jobs (via {@link com.bakdata.conquery.models.jobs.CalculateCBlocksJob}) in short succession, which will cause it to sleep. This field helps alleviate memory pressure on the Shards by slowing down the Manager, should it be sending too fast.
	 */
	@Min(0)
	private int backpressure = 1500;
}
