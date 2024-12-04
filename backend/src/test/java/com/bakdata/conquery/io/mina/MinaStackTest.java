package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.MinaConfig;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.jupiter.api.Test;

public class MinaStackTest {

	@Test
	void test() throws IOException {

		// Server
		NioSocketAcceptor acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addFirst("mdc", new MdcFilter("Manager[%s]"));

		final ObjectMapper om = Jackson.BINARY_MAPPER.copy();

		ProtocolCodecFilter codecfilter = new ProtocolCodecFilter(
				new JacksonProtocolEncoder(om.writerFor(NetworkMessage.class)),
				new JacksonProtocolDecoder(om.readerFor(NetworkMessage.class))
		);
		acceptor.getFilterChain().addLast("codec", codecfilter);
		acceptor.setHandler(new IoHandlerAdapter() {

		});

		MinaConfig minaConfig = new MinaConfig();
		acceptor.getSessionConfig().setAll(minaConfig);
		InetSocketAddress inboundAddress = new InetSocketAddress(0);
		acceptor.bind(inboundAddress);

		// Client
//		final InetSocketAddress address = new InetSocketAddress(
//				config.getCluster().getManagerURL().getHostAddress(),
//				config.getCluster().getPort()
//		);
//
//
//		connector = getClusterConnector();
//
//		while (true) {
//			try {
//				log.info("Trying to connect to {}", address);
//
//				// Try opening a connection (Note: This fails immediately instead of waiting a minute to try and connect)
//				future = connector.connect(address);
//
//				future.awaitUninterruptibly();
//
//				if (future.isConnected()) {
//					break;
//				}
//
//				future.cancel();
//				// Sleep thirty seconds then retry.
//				TimeUnit.SECONDS.sleep(config.getCluster().getConnectRetryTimeout().toSeconds());
//
//			}
//			catch (RuntimeIoException e) {
//				log.warn("Failed to connect to {}", address, e);
//			}
//			catch (InterruptedException e) {
//				log.warn("Interrupted while trying to connector to cluster, giving up.", e);
//				break;
//			}
//		}
	}
}
