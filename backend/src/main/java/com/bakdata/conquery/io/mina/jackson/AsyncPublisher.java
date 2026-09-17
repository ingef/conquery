package com.bakdata.conquery.io.mina.jackson;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;

/**
 * Ensures in-order publication of incoming messages.
 * <p>
 * Primarily relevant for messages of varying sizes.
 */
@Slf4j
@Data
class AsyncPublisher {
	private final IoSession session;
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final BlockingQueue<CompletableFuture<FinishedMessage>> finishedMessages = new LinkedBlockingQueue<>();


	/**
	 * Signals thread to drain queue and close.
	 */
	public void close() {
		log.debug("Closing session publisher");
		running.set(false);
	}

	/**
	 * Registers a reader with this Publisher.
	 * This ensures the reader's results are published in order of arrival.
	 */
	public void register(AsyncReader reader) {
		assert reader.getFutureMessage() == null;

		log.trace("Registering {}", reader);

		final CompletableFuture<FinishedMessage> future = new CompletableFuture<>();

		reader.setFutureMessage(future);
		finishedMessages.add(future);
	}

	/**
	 * Ensures publishing of arrived messages in order, despite potential timing differences in serialization speed.
	 */
	public void publish() {
		while (!finishedMessages.isEmpty() || running.get()) {
			try {
				log.trace("BEGIN polling for FinishedMessage, currently {} queued.", finishedMessages.size());

				// The timeout ensures we actually see running=false and shutdown.
				final Future<FinishedMessage> readerFuture = finishedMessages.poll(5, TimeUnit.SECONDS);

				if (readerFuture == null) {
					// If timed out, we receive null.
					continue;
				}

				log.trace("BEGIN waiting for {}", readerFuture);

				final FinishedMessage reader = readerFuture.get();
				log.trace("RECEIVED {} from {}", reader, reader);

				reader.handleMessage(session);
			}
			catch (InterruptedException e) {
				Thread.interrupted();
				log.trace("Interrupted", e);
			}
			catch (ExecutionException e) {
				log.error("Failed to get reader", e);
			}
		}
	}

	/**
	 * Wrapper for bundling of {@link org.apache.mina.core.filterchain.IoFilter.NextFilter} and {@link NetworkMessage}.
	 */
	public record FinishedMessage(IoFilter.NextFilter nextFilter, NetworkMessage networkMessage) {
		public void handleMessage(IoSession session) {
			try {
				nextFilter.messageReceived(session, networkMessage);
			}
			catch (Exception e) {
				log.error("{} FAILED to deliver message {}", session, networkMessage, e);
			}
		}
	}
}
