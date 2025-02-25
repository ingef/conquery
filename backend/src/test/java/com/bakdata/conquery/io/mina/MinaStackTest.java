package com.bakdata.conquery.io.mina;

import static java.lang.Math.toIntExact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
		CLUSTER_CONFIG.setMaxIoBufferSizeBytes(toIntExact(DataSize.mebibytes(10).toBytes()));

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
												   }, "Server"
		);

	}

	private static Stream<Arguments> dataSizes() {
		return Stream.of(
				Arguments.of(DataSize.bytes(10), true),
				Arguments.of(DataSize.kibibytes(10), true),
				Arguments.of(DataSize.mebibytes(9), true),
				Arguments.of(DataSize.mebibytes(10), true)
		);
	}

	@AfterAll
	public static void afterAll() {
		SERVER.dispose();
	}

	@BeforeEach
	public void beforeEach() {
		SERVER_RECEIVED_MESSAGES.clear();
	}

	@Test
	void smokeTest() {

		final NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
																				 @Override
																				 public void sessionOpened(IoSession session) {
																					 log.info("Session to {} established", session.getRemoteAddress());
																				 }
																			 }, "Client"
		);

		try {

			final ConnectFuture connect = client.connect(SERVER.getLocalAddress());

			connect.awaitUninterruptibly();
			final IoSession clientSession = connect.getSession();

			final NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(1000));

			final WriteFuture write = clientSession.write(input);

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
	void concurrentWriting() {
		final int clientCount = 20;
		final int messagesPerClient = 5;
		final int minMessageLength = toIntExact(DataSize.kibibytes(1).toBytes());
		final int maxMessageLength = toIntExact(DataSize.kibibytes(100).toBytes());

		final ConcurrentLinkedQueue<NetworkMessage<?>> messagesWritten = new ConcurrentLinkedQueue<>();
		final List<CompletableFuture<?>> clientThreads = new ArrayList<>();

		final ExecutorService executorService = Executors.newFixedThreadPool(clientCount);
		try {
			for (int clientI = 0; clientI < clientCount; clientI++) {
				final int clientNumber = clientI;
				final CompletableFuture<?> clientThread = CompletableFuture.runAsync(() -> {
																						 final NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
																																								  @Override
																																								  public void sessionOpened(IoSession session) {
																																									  log.info("Session to {} established", session.getRemoteAddress());
																																								  }

																																								  @Override
																																								  public void messageSent(IoSession session, Object message) {
																																									  log.trace("Message written: {} bytes", ((TestMessage) message).data.getBytes().length);
																																								  }

																																								  @Override
																																								  public void exceptionCaught(IoSession session, Throwable cause) {
																																									  fail("Client[%d] caught an Exception".formatted(clientNumber), cause);
																																								  }
																																							  }, "Client"
																						 );
																						 try {
																							 // Connect
																							 final ConnectFuture connect = client.connect(SERVER.getLocalAddress());
																							 connect.awaitUninterruptibly();
																							 final IoSession clientSession = connect.getSession();

																							 for (int i = 0; i < messagesPerClient; i++) {
																								 final NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(minMessageLength, maxMessageLength));

																								 final WriteFuture writeFuture = clientSession.write(input);
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
																					 }, executorService
				);
				clientThreads.add(clientThread);
			}

			// Wait until all clients completed writing
			CompletableFuture.allOf(clientThreads.toArray(new CompletableFuture[0])).join();

			log.info("Waiting to receive all send messages");
			// Wait until all messages are received
			await().atMost(10, TimeUnit.SECONDS)
				   .alias(String.format("Send and received same amount of messages (%s < %s)", SERVER_RECEIVED_MESSAGES.size(), messagesWritten.size()))
				   .until(() -> SERVER_RECEIVED_MESSAGES.size() >= messagesWritten.size());

			// Check that the messages are correct
			assertThat(SERVER_RECEIVED_MESSAGES).containsExactlyInAnyOrderElementsOf(messagesWritten);

		}
		finally {
			executorService.shutdownNow();
		}

	}

	@ParameterizedTest
	@MethodSource("dataSizes")
	void messageSizes(DataSize dataSize, boolean shouldPass) {
		final NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
																				 @Override
																				 public void sessionOpened(IoSession session) {
																					 log.info("Session to {} established", session.getRemoteAddress());
																				 }

																				 @Override
																				 public void exceptionCaught(IoSession session, Throwable cause) {
																					 log.trace("Failed to write message (probably expected)", cause);
																				 }
																			 }, "Client"
		);

		try {

			final ConnectFuture connect = client.connect(SERVER.getLocalAddress());

			connect.awaitUninterruptibly();
			final IoSession clientSession = connect.getSession();

			final NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(toIntExact(dataSize.toBytes())));

			final WriteFuture write = clientSession.write(input);

			write.awaitUninterruptibly();

			assertThat(write.isWritten())
					.describedAs(() -> write.getException().toString())
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

	//	TODO @Test
	//	void differentMessageTypes() {
	//
	//		NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
	//																		   @Override
	//																		   public void sessionOpened(IoSession session) {
	//																			   log.info("Session to {} established", session.getRemoteAddress());
	//																		   }
	//																	   }, "Client"
	//		);
	//
	//		try {
	//
	//			ConnectFuture connect = client.connect(SERVER.getLocalAddress());
	//
	//			connect.awaitUninterruptibly();
	//			IoSession clientSession = connect.getSession();
	//
	//			NetworkMessage<?> input1 = new TestMessage(RandomStringUtils.randomAscii(1000));
	//			WriteFuture write1 = clientSession.write(input1);
	//
	//			NetworkMessage<?> input2 = ForwardToWorker.create(new WorkerId(new DatasetId("dataset"), "worker"), new RequestConsistency());
	//			WriteFuture write2 = clientSession.write(input2);
	//
	//			write1.awaitUninterruptibly();
	//			write2.awaitUninterruptibly();
	//
	//			await().atMost(1, TimeUnit.MINUTES).until(() -> !SERVER_RECEIVED_MESSAGES.isEmpty());
	//			assertThat(SERVER_RECEIVED_MESSAGES)
	//					.containsExactlyInAnyOrder(input1, input2)
	//					.usingRecursiveComparison();
	//
	//			clientSession.closeNow().awaitUninterruptibly();
	//		}
	//		finally {
	//			client.dispose();
	//
	//		}
	//	}

	/**
	 * This test requires a little RAM because we hold the messages twice to compare sender and receiver payloads.
	 */
	@Test
	void edgeBufferingTest() {
		final int messagesPerClient = 5000;
		// 64Kibi is the max size of a buffer ON MY MACHINE.
		// 57 is number of bytes in an empty TestMessage.
		// 7+2 were tested to create the most edge-buffering. It's still well below 1% in this setup.
		final int minMessageLength = toIntExact(DataSize.kibibytes(64).toBytes() - 7 - 57);
		final int maxMessageLength = minMessageLength + 2;

		final ConcurrentLinkedQueue<NetworkMessage<?>> messagesWritten = new ConcurrentLinkedQueue<>();

		final NioSocketConnector client = CLUSTER_CONFIG.getClusterConnector(OM, new IoHandlerAdapter() {
																				 @Override
																				 public void sessionOpened(IoSession session) {
																					 log.info("Session to {} established", session.getRemoteAddress());
																				 }

																				 @Override
																				 public void messageSent(IoSession session, Object message) {
																					 log.trace("Message written: {} bytes", ((TestMessage) message).data.getBytes().length);
																				 }

																				 @Override
																				 public void exceptionCaught(IoSession session, Throwable cause) {
																					 fail("Client caught an Exception", cause);
																				 }
																			 }, "Client"
		);
		try {

			// Connect
			final ConnectFuture connect = client.connect(SERVER.getLocalAddress());
			connect.awaitUninterruptibly();
			final IoSession clientSession = connect.getSession();

			for (int i = 0; i < messagesPerClient; i++) {
				final NetworkMessage<?> input = new TestMessage(RandomStringUtils.randomAscii(minMessageLength, maxMessageLength));

				final WriteFuture writeFuture = clientSession.write(input);
				writeFuture.addListener((f) -> {
					if (!((WriteFuture) f).isWritten()) {
						fail("Failed to write a message");
					}
					messagesWritten.add(input);
				});
				writeFuture.awaitUninterruptibly();
			}

			log.info("Waiting to receive all send messages");
			// Wait until all messages are received
			await().atMost(10, TimeUnit.SECONDS)
				   .alias(String.format("Send and received same amount of messages (%s < %s)", SERVER_RECEIVED_MESSAGES.size(), messagesWritten.size()))
				   .until(() -> SERVER_RECEIVED_MESSAGES.size() >= messagesWritten.size());

			// Check that the messages are correct
			assertThat(SERVER_RECEIVED_MESSAGES).containsExactlyInAnyOrderElementsOf(messagesWritten);

		}
		finally {
			client.dispose();
		}

	}

	public static class TestNetworkMessageContext extends NetworkMessageContext<TestMessage> {

		public TestNetworkMessageContext(NetworkSession session) {
			super(session, 0);
		}
	}

	@CPSType(id = "TEST_MSG", base = NetworkMessage.class)
	@RequiredArgsConstructor
	@Getter
	@EqualsAndHashCode(callSuper = false)
	public static class TestMessage extends NetworkMessage<TestNetworkMessageContext> {

		private final String data;

		@JsonCreator
		public TestMessage(String data, UUID messageId) {
			setMessageId(messageId);
			this.data = data;
		}

		@Override
		public void react(TestNetworkMessageContext context) {
			// Do nothing
		}
	}
}
