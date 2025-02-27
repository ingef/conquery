package com.bakdata.conquery.io.mina.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.DataSize;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;

/**
 * Uses {@link PipedInputStream} / {@link PipedOutputStream} to do the actual reading of incoming values in a separate thread.
 * <p>
 * Messages may arrive in variable sized chunks therefore we need to track remaining data.
 */
@Slf4j
@Data
class AsyncReader {

	private final ObjectMapper mapper;
	private final PipedOutputStream outputStream;
	private final InputStream inputStream;

	private final IoFilter.NextFilter nextFilter;

	private int remaining;
	private CompletableFuture<AsyncPublisher.FinishedMessage> futureMessage;


	@SneakyThrows
	public AsyncReader(int remaining, ObjectMapper mapper, IoFilter.NextFilter nextFilter, DataSize bufferSize) {
		this.mapper = mapper;
		this.nextFilter = nextFilter;
		this.remaining = remaining;

		outputStream = new PipedOutputStream();
		inputStream = new PipedInputStream(outputStream, ((int) bufferSize.toBytes()));
	}

	/**
	 * Read from incoming buffer into outputStream. Ensures we never read more than remaining.
	 */
	public void receive(IoBuffer buffer) throws IOException {
		final int reading = Math.min(buffer.remaining(), remaining);

		final IoBuffer slice = buffer.getSlice(reading);
		try (InputStream inputStream = slice.asInputStream()) {
			// This stupid method mutates BOTH buffer and slice WTF.
			final long written = inputStream.transferTo(getOutputStream());
			remaining -= (int) written;
		}

		assert remaining >= 0 : "Transferred more than we required to read the message.";

		if (hasEnough()) {
			getOutputStream().close();
		}
	}

	public boolean hasEnough() {
		return remaining <= 0;
	}

	@SneakyThrows
	public void read() {
		try {
			// This thread can and will pause when inputStream does not have enough data.
			final Stopwatch timer = Stopwatch.createStarted();

			final NetworkMessage result = mapper.readValue(inputStream, NetworkMessage.class);
			log.trace("FINISHED parsing {} within {}", result, timer);

			futureMessage.complete(new AsyncPublisher.FinishedMessage(nextFilter, result));
		}
		catch (IOException e) {
			futureMessage.completeExceptionally(e);
			log.error("Something went wrong.", e);
		}
	}
}
