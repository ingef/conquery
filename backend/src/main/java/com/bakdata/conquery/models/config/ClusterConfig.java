package com.bakdata.conquery.models.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.MdcFilter;
import com.bakdata.conquery.io.mina.PipedJacksonProtocolFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.PortRange;
import lombok.Getter;
import lombok.Setter;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

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
	private DefaultSocketSessionConfig mina = new DefaultSocketSessionConfig();
	@Min(1)
	private int entityBucketSize = 1000;

	private Duration idleTimeOut = Duration.minutes(5);
	private Duration heartbeatTimeout = Duration.minutes(1);
	private Duration connectRetryTimeout = Duration.seconds(30);

	/**
	 * Defines the maximum buffer size inclusive 4 bytes for a header. Objects larger than this size cannot be sent over the cluster.
	 * <p/>
	 * May only touch this for testing purposes.
	 */
	@Max(Integer.MAX_VALUE - 4)
	@Min(64) // not practical
	private int maxIoBufferSizeBytes = Integer.MAX_VALUE - 4;

	/**
	 * Defines the starting buffer allocation size. Larger can reduce reallocations, but can cause a greater memory demand.
	 * <p/>
	 * May only touch this for testing purposes.
	 */
	@Max(Integer.MAX_VALUE - 4)
	@Min(64) // Mina's default
	private int initialIoBufferSizeBytes = 8192; // 8kb

	/**
	 * @see com.bakdata.conquery.models.messages.namespaces.specific.CollectColumnValuesJob
	 * <p>
	 * Number of values to batch for chunking of unique column-values. Lower numbers reduce relative performance but reduce memory demand, avoiding OOM issues.
	 */
	private int columnValuesPerChunk = 1000;

	/**
	 * @see com.bakdata.conquery.io.mina.NetworkSession
	 * <p>
	 * Maximum number of messages allowed to wait for writing before writer-threads are blocked.
	 */
	private int networkSessionMaxQueueLength = 5;

	/**
	 * Amount of backpressure before jobs can volunteer to block to send messages to their shards.
	 * <p>
	 * Mostly {@link com.bakdata.conquery.models.jobs.ImportJob} is interested in this. Note that an average import should create more than #Entities / {@linkplain #entityBucketSize} jobs (via {@link com.bakdata.conquery.models.jobs.CalculateCBlocksJob}) in short succession, which will cause it to sleep. This field helps alleviate memory pressure on the Shards by slowing down the Manager, should it be sending too fast.
	 */
	@Min(0)
	private int backpressure = 1500;

	@JsonIgnore
	public NioSocketConnector getClusterConnector(ObjectMapper om, IoHandler ioHandler, String mdcLocation) {
		om = om.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

		final NioSocketConnector connector = new NioSocketConnector();


		IoFilter codecFilter = new PipedJacksonProtocolFilter("shard_" + mdcLocation, om);

		connector.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocation));
//		if (mina.getSendBufferSize() > 0) {
//			connector.getFilterChain().addLast("chunk", new ChunkingFilter(mina.getSendBufferSize()));
//		}
		connector.getFilterChain().addLast("codec", codecFilter);

		connector.setHandler(ioHandler);
		connector.getSessionConfig().setAll(getMina());

		return connector;
	}

	@JsonIgnore
	public NioSocketAcceptor getClusterAcceptor(ObjectMapper om, IoHandler ioHandler, String mdcLocation) throws IOException {
		om = om.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

		NioSocketAcceptor acceptor = new NioSocketAcceptor();

		IoFilter codecFilter = new PipedJacksonProtocolFilter("manager" + mdcLocation, om);

		acceptor.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocation));
//		if (mina.getSendBufferSize() > 0) {
//			acceptor.getFilterChain().addLast("chunk", new ChunkingFilter(mina.getSendBufferSize()));
//		}
		acceptor.getFilterChain().addLast("codec", codecFilter);

		acceptor.setHandler(ioHandler);
		acceptor.getSessionConfig().setAll(getMina());
		acceptor.bind(new InetSocketAddress(getPort()));

		return acceptor;
	}
}
