package com.bakdata.conquery.models.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.JacksonProtocolDecoder;
import com.bakdata.conquery.io.mina.JacksonProtocolEncoder;
import com.bakdata.conquery.io.mina.MdcFilter;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.PortRange;
import lombok.Getter;
import lombok.Setter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
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
	private MinaConfig mina = new MinaConfig();
	@Min(1)
	private int entityBucketSize = 1000;

	private Duration idleTimeOut =  Duration.minutes(5);
	private Duration heartbeatTimeout = Duration.minutes(1);
	private Duration connectRetryTimeout = Duration.seconds(30);

	/**
	 * @see com.bakdata.conquery.models.messages.namespaces.specific.CollectColumnValuesJob
	 *
	 * Number of values to batch for chunking of unique column-values. Lower numbers reduce relative performance but reduce memory demand, avoiding OOM issues.
	 */
	private int columnValuesPerChunk = 1000;

	/**
	 * @see com.bakdata.conquery.io.mina.NetworkSession
	 *
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
	public NioSocketConnector getClusterConnector(ObjectMapper om, IoHandler ioHandler, String mdcLocationFmt) {

		final NioSocketConnector connector = new NioSocketConnector();

		ProtocolCodecFilter codecFilter = new ProtocolCodecFilter(
				new JacksonProtocolEncoder(om.writerFor(NetworkMessage.class)),
				new JacksonProtocolDecoder(om.readerFor(NetworkMessage.class))
		);
		connector.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocationFmt));
		connector.getFilterChain().addLast("codec", codecFilter);
		connector.setHandler(ioHandler);
		connector.getSessionConfig().setAll(getMina());
		return connector;
	}

	@JsonIgnore
	public NioSocketAcceptor getClusterAcceptor(ObjectMapper om, IoHandler ioHandler, String mdcLocationFmt) throws IOException {
		NioSocketAcceptor acceptor = new NioSocketAcceptor();

		acceptor.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocationFmt));
		ProtocolCodecFilter codecFilter = new ProtocolCodecFilter(
				new JacksonProtocolEncoder(om.writerFor(NetworkMessage.class)),
				new JacksonProtocolDecoder(om.readerFor(NetworkMessage.class))
		);
		acceptor.getFilterChain().addLast("codec", codecFilter);

		acceptor.setHandler(ioHandler);
		acceptor.getSessionConfig().setAll(getMina());
		acceptor.bind(new InetSocketAddress(getPort()));

		return acceptor;
	}
}
