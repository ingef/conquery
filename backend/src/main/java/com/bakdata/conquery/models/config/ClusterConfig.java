package com.bakdata.conquery.models.config;

import java.net.InetAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.dropwizard.core.Configuration;
import io.dropwizard.util.DataSize;
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
	private Duration heartbeatTimeout = Duration.minutes(1);
	private Duration connectRetryTimeout = Duration.seconds(30);

	/**
	 * {@link org.apache.mina.core.buffer.IoBuffer} size, that mina allocates.
	 * We assume a pagesize of 4096 bytes == 4 kibibytes
	 */
	@NotNull
	@Valid
	private DataSize messageChunkSize = DataSize.kibibytes(4);

	/**
	 * How long the soft pool cleaner waits before reducing the pool size down to softPoolBaselineSize.
	 */
	@NotNull
	@Valid
	private Duration softPoolCleanerPause = Duration.seconds(10);

	/**
	 * The number of soft references the soft pool should retain after cleaning.
	 * The actual number of {@link org.apache.mina.core.buffer.IoBuffer}
	 */
	private long softPoolBaselineSize = 100;

	/**
	 * Amount of backpressure before jobs can volunteer to block to send messages to their shards.
	 * <p>
	 * Mostly {@link com.bakdata.conquery.models.jobs.ImportJob} is interested in this. Note that an average import should create more than #Entities / {@linkplain #entityBucketSize} jobs (via {@link com.bakdata.conquery.models.jobs.CalculateCBlocksJob}) in short succession, which will cause it to sleep. This field helps alleviate memory pressure on the Shards by slowing down the Manager, should it be sending too fast.
	 */
	@Min(0)
	private int backpressure = 1500;
}
