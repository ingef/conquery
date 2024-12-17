package com.bakdata.conquery.io.mina;

import static java.lang.Math.toIntExact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ClusterConfig;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.NetworkMessageContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.util.DataSize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class MinaStackTest {

	private static final ClusterConfig CLUSTER_CONFIG = new ClusterConfig();
	private static final ObjectMapper OM = Jackson.BINARY_MAPPER.copy();
	private static final ConcurrentLinkedQueue<NetworkMessage<?>> SERVER_RECEIVED_MESSAGES = new ConcurrentLinkedQueue<>();

	private static NioSocketAcceptor SERVER;

	@BeforeAll
	public static void beforeAll() throws IOException {

		CLUSTER_CONFIG.setPort(0);
		CLUSTER_CONFIG.setMaxIoBufferSize(DataSize.mebibytes(10));

		// This enables the Chunking filter, which triggers for messages > 1 MebiByte
		CLUSTER_CONFIG.getMina().setSendBufferSize(toIntExact(DataSize.mebibytes(1).toBytes()));

		// Server
		SERVER = CLUSTER_CONFIG.getClusterAcceptor(OM, new IoHandlerAdapter() {
			@Override
			public void sessionOpened(IoSession session) {
				log.info("Session to {} established", session.getRemoteAddress());
			}

			@Override
			public void messageReceived(IoSession session, Object message) {
				SERVER_RECEIVED_MESSAGES.add((NetworkMessage<?>) message);
				log.trace("Received {} messages", SERVER_RECEIVED_MESSAGES.size());
			}

			@Override
			public void exceptionCaught(IoSession session, Throwable cause) {
				fail("Server caught an Exception", cause);
			}
		}, "Server");

	}

	@BeforeEach
	public void beforeEach() {
		SERVER_RECEIVED_MESSAGES.clear();
	}

	@Test
	void smokeTest() {

		NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
			@Override
			public void sessionOpened(IoSession session) {
				log.info("Session to {} established", session.getRemoteAddress());
			}
		}, "Client");

		try {

			ConnectFuture connect = client.connect(SERVER.getLocalAddress());

			connect.awaitUninterruptibly();
			IoSession clientSession = connect.getSession();

			NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(1000));

			WriteFuture write = clientSession.write(input);

			write.awaitUninterruptibly();

			await().atMost(1, TimeUnit.SECONDS).until(() -> !SERVER_RECEIVED_MESSAGES.isEmpty());
			assertThat(SERVER_RECEIVED_MESSAGES).containsExactlyInAnyOrder(input);

			clientSession.closeNow().awaitUninterruptibly();
		}
		finally {
			client.dispose();

		}
	}

	/**
	 * This test requires a little RAM because we hold the messages twice to compare sender and receiver payloads.
	 */
	@Test
	void concurrentWriting(){
		final int clientCount = 20;
		final int messagesPerClient = 500;
		final int minMessageLength = toIntExact(DataSize.kibibytes(1).toBytes());
		final int maxMessageLength = toIntExact(DataSize.kibibytes(100).toBytes());

		final ConcurrentLinkedQueue<NetworkMessage<?>> messagesWritten = new ConcurrentLinkedQueue<>();
		final List<CompletableFuture<?>> clientThreads = new ArrayList<>();

		ExecutorService executorService = Executors.newFixedThreadPool(10);
		try {
			for (int clientI = 0; clientI < clientCount; clientI++) {
				final int clientNumber = clientI;
				CompletableFuture<?> clientThread = CompletableFuture.runAsync(() -> {
					NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
						@Override
						public void sessionOpened(IoSession session) {
							log.info("Session to {} established", session.getRemoteAddress());
						}

						@Override
						public void messageSent(IoSession session, Object message) {
							log.trace("Message written: {} bytes", ((TestMessage)message).data.getBytes().length);
						}

						@Override
						public void exceptionCaught(IoSession session, Throwable cause) {
							fail("Client[%d] caught an Exception".formatted(clientNumber), cause);
						}
					}, "Client");
					try {
						// Connect
						ConnectFuture connect = client.connect(SERVER.getLocalAddress());
						connect.awaitUninterruptibly();
						IoSession clientSession = connect.getSession();

						for (int i = 0; i < messagesPerClient; i++) {
							NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(minMessageLength, maxMessageLength));

							WriteFuture writeFuture = clientSession.write(input);
							writeFuture.addListener((f) -> {
								if (!((WriteFuture) f).isWritten()) {
									fail("Failed to write a message");
								}
								messagesWritten.add(input);
							});
							writeFuture.awaitUninterruptibly();
						}
					}
					finally {
						client.dispose();
					}
				}, executorService);
				clientThreads.add(clientThread);
			}

			// Wait until all clients completed writing
			CompletableFuture.allOf(clientThreads.toArray(new CompletableFuture[0])).join();

			// Wait until all messages are received
			await().atMost(10,TimeUnit.SECONDS).until(() -> SERVER_RECEIVED_MESSAGES.size() == messagesWritten.size());

			// Check that the messages are correct
			assertThat(SERVER_RECEIVED_MESSAGES).containsExactlyInAnyOrderElementsOf(messagesWritten);

		}
		finally {
			executorService.shutdownNow();
		}

	}

	private static Stream<Arguments> dataSizes() {
		return Stream.of(
				Arguments.of(DataSize.bytes(10), true),
				Arguments.of(DataSize.kibibytes(10), true),
				Arguments.of(DataSize.mebibytes(9), true), // Uses chunking
				Arguments.of(DataSize.mebibytes(10), false) // Is too large for jackson encoder
		);
	}

	@ParameterizedTest
	@MethodSource("dataSizes")
	void messageSizes(DataSize dataSize, boolean shouldPass) {
		NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
			@Override
			public void sessionOpened(IoSession session) {
				log.info("Session to {} established", session.getRemoteAddress());
			}

			@Override
			public void exceptionCaught(IoSession session, Throwable cause) {
				log.trace("Failed to write message (probably expected)",cause);
			}
		}, "Client");

		try {

			ConnectFuture connect = client.connect(SERVER.getLocalAddress());

			connect.awaitUninterruptibly();
			IoSession clientSession = connect.getSession();

			NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(toIntExact(dataSize.toBytes())));

			WriteFuture write = clientSession.write(input);

			write.awaitUninterruptibly();

			assertThat(write.isWritten())
					.describedAs(() -> write.getException().getMessage())
					.isEqualTo(shouldPass);

			Assertions.setMaxStackTraceElementsDisplayed(200);
			if (!shouldPass) {
				assertThat(write.getException()).hasCauseInstanceOf(IllegalArgumentException.class);
			}

			clientSession.closeNow().awaitUninterruptibly();
		}
		finally {
			client.dispose();

		}
	}

	@AfterAll
	public static void afterAll() {
		SERVER.dispose();
	}

	public static class TestNetworkMessageContext extends NetworkMessageContext<TestMessage> {

		public TestNetworkMessageContext(NetworkSession session) {
			super(session, 0);
		}
	}

	@CPSType(id = "TEST_MSG", base = NetworkMessage.class)
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	@Getter
	@EqualsAndHashCode(callSuper = false)
	public static class TestMessage extends NetworkMessage<TestNetworkMessageContext> {

		private final String data;

		@Override
		public void react(TestNetworkMessageContext context) {
			// Do nothing
		}
	}
}
