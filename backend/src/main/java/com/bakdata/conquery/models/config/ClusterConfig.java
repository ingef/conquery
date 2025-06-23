package com.bakdata.conquery.models.config;

import com.bakdata.conquery.io.mina.MdcFilter;
import com.bakdata.conquery.io.mina.jackson.AsyncJacksonProtocolFilter;
import com.bakdata.conquery.models.messages.namespaces.specific.CollectColumnValuesMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Configuration;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.DataSizeRange;
import io.dropwizard.validation.PortRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
	 * Defines the starting buffer allocation size. Larger can reduce reallocations, but can cause a greater memory demand.
	 * <p/>
	 * May only touch this for testing purposes.
	 */
	@DataSizeRange(min = 64, max = Integer.MAX_VALUE - 4)
	private DataSize initialIoBufferSize = DataSize.bytes(64);

	/**
	 * @see CollectColumnValuesMessage
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

	@JsonIgnore
	public NioSocketConnector getClusterConnector(ObjectMapper om, IoHandler ioHandler, String mdcLocation) {

		final NioSocketConnector connector = new NioSocketConnector();

		final IoFilter codecFilter = new AsyncJacksonProtocolFilter(om, (int) initialIoBufferSize.toBytes());


		connector.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocation));
		connector.getFilterChain().addLast("codec", codecFilter);

		connector.setHandler(ioHandler);
		connector.getSessionConfig().setAll(getMina());

		return connector;
	}

	@JsonIgnore
	public NioSocketAcceptor getClusterAcceptor(ObjectMapper om, IoHandler ioHandler, String mdcLocation) throws IOException {

		final NioSocketAcceptor acceptor = new NioSocketAcceptor();

		final IoFilter codecFilter = new AsyncJacksonProtocolFilter(om, (int) initialIoBufferSize.toBytes());

		acceptor.getFilterChain().addFirst("mdc", new MdcFilter(mdcLocation));
		acceptor.getFilterChain().addLast("codec", codecFilter);

		acceptor.setHandler(ioHandler);
		acceptor.getSessionConfig().setAll(getMina());
		acceptor.bind(new InetSocketAddress(getPort()));

		return acceptor;
	}
}
