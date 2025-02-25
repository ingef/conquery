package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.DataSize;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PipedJacksonProtocolFilter extends IoFilterAdapter {
	private static final AttributeKey READER_KEY = new AttributeKey(PipedJacksonProtocolFilter.class, "reader");
	private static final AttributeKey EDGE_BUFFER = new AttributeKey(PipedJacksonProtocolFilter.class, "edge");


	private final ObjectMapper mapper;

	private final ExecutorService readerThreads = Executors.newCachedThreadPool((runnable) -> {
		Thread thread = Thread.ofVirtual().name("%s#reader".formatted(getClass().getSimpleName())).unstarted(runnable);

		thread.setDaemon(true);
		return thread;
	});


	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		final IoBuffer buffer = (IoBuffer) message;


		synchronized (session) {
			do {
				Reader asyncReader = getAsyncReader(session);

				if (asyncReader == null) {
					if (requiresEdgeBuffering(session, buffer)) {
						break;
					}

					final int remaining = getBufferLength(session, buffer);

					asyncReader = newAsyncReader(remaining, nextFilter, session);
					log.trace("Starting new reader for {}", DataSize.bytes(remaining));
				}
				else {
					log.trace("Continuing with {}", DataSize.bytes(asyncReader.getRemaining()));
				}

				final boolean finished = asyncReader.receive(buffer);

				if (finished) {
					session.setAttribute(READER_KEY, null);
				}

			} while (buffer.hasRemaining());
		}
	}

	private Reader getAsyncReader(IoSession session) {
		return (Reader) session.getAttribute(READER_KEY);
	}

	/**
	 * Test if we have enough contents to read the incoming buffer's length.
	 * If not, cumulate the remaining buffer into a new buffer.
	 *
	 * Incoming buffers will combine their remaining part of the prefix-length.
	 */
	private boolean requiresEdgeBuffering(IoSession session, IoBuffer buffer) throws IOException {
		if (buffer.remaining() >= Integer.BYTES) {
			return false;
		}

		log.trace("Not enough content in current buffer ({} bytes), resorting to edge-buffering.", buffer.remaining());

		final IoBuffer edgeBuffer = IoBuffer.allocate(Integer.BYTES, false);
		edgeBuffer.setAutoExpand(false);

		buffer.asInputStream().transferTo(edgeBuffer.asOutputStream());

		session.setAttribute(EDGE_BUFFER, edgeBuffer);

		assert !buffer.hasRemaining();

		return true;
	}

	private static int getBufferLength(IoSession session, IoBuffer buffer) throws IOException {
		final IoBuffer edgeBuffer;
		synchronized (session) {
			edgeBuffer = (IoBuffer) session.setAttribute(EDGE_BUFFER, null);

			if (edgeBuffer == null) {
				return buffer.getInt();
			}
		}

		log.trace("Found existing edge-buffer {}, incoming {}", DataSize.bytes(edgeBuffer.position()), DataSize.bytes(buffer.limit()));

		buffer.getSlice(Integer.BYTES - edgeBuffer.position())
			  .asInputStream()
			  .transferTo(edgeBuffer.asOutputStream());

		edgeBuffer.flip();

		assert edgeBuffer.remaining() == Integer.BYTES;

		return edgeBuffer.getInt();
	}

	@NotNull
	private Reader newAsyncReader(int remaining, NextFilter nextFilter, IoSession session) {
		final Reader reader = new Reader(remaining, mapper, nextFilter, session);

		readerThreads.submit(reader::read);

		session.setAttribute(READER_KEY, reader);
		return reader;
	}

	@Override
	public synchronized void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
		//TODO use CachedBufferAllocator?
		final IoBuffer buf = IoBuffer.allocate(64, false);
		buf.setAutoExpand(true);
		buf.putInt(0); //DUMMY


		final Stopwatch stopwatch = Stopwatch.createStarted();
		log.trace("BEGIN Encoding message");

		final Object message = writeRequest.getMessage();
		try (OutputStream outputStream = buf.asOutputStream()) {
			mapper.writerFor(NetworkMessage.class).writeValue(outputStream, message);
		}

		log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), message);
		buf.putInt(0, buf.position() - Integer.BYTES);
		buf.flip();

		writeRequest.setMessage(buf);

		nextFilter.filterWrite(session, writeRequest);
	}

	@Data
	private class Reader {

		private final ObjectMapper mapper;
		private final PipedOutputStream outputStream;
		private final InputStream inputStream;
		private final NextFilter nextFilter;
		private final IoSession session;
		private int remaining;

		@SneakyThrows
		public Reader(int remaining, ObjectMapper mapper, NextFilter nextFilter, IoSession session) {
			this.mapper = mapper;
			this.nextFilter = nextFilter;
			this.session = session;
			this.remaining = remaining;
			outputStream = new PipedOutputStream();
			inputStream = new PipedInputStream(outputStream, ((int) DataSize.kilobytes(64).toBytes()));
		}

		public boolean receive(IoBuffer buffer) throws IOException {
			final int reading = Math.min(buffer.remaining(), remaining);

			final IoBuffer slice = buffer.getSlice(reading);
			try (InputStream inputStream = slice.asInputStream()) {
				// This stupid method mutates BOTH buffer and slice WTF.
				final long written = inputStream.transferTo(getOutputStream());
				remaining -= (int) written;
			}

			assert remaining >= 0 : "Transferred more than we required to read the message.";

			if (remaining == 0) {
				getOutputStream().close();
				return true;
			}

			return false;
		}

		public void read() {
			try {
				final NetworkMessage parsed = mapper.readValue(inputStream, NetworkMessage.class);
				log.trace("Received {}", parsed);

				nextFilter.messageReceived(session, parsed);
			}
			catch (IOException e) {
				log.error("Something went wrong reading from {}", session, e);
			}
		}
	}


}
