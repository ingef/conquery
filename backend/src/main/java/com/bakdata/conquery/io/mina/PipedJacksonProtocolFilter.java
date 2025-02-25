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

/**
 * This {@link org.apache.mina.core.filterchain.IoFilter} (de-)serializes POJOs using a Jackson {@link ObjectMapper}.
 * <p>
 * Since we can send very large messages we need to handle combination of multiple messages into one.
 * To avoid costly buffering in memory we instead pass incoming buffers to an asynchronous reader thread, which handles the actual reading.
 * <p>
 * In comparison {@link org.apache.mina.filter.codec.CumulativeProtocolDecoder} can end up at exponential runtime when messages become too large.
 * <p>
 * brief explanation of the reader implementation:
 * <p>
 * 1) {@link PipedJacksonProtocolFilter#filterWrite(NextFilter, IoSession, WriteRequest)} serializes every message using the provided {@link ObjectMapper}, the length of the written contents is prefixed to the contents.
 * 2) This buffer is then transmitted by the Processor. There are virtually no guarantees about transmission except the buffers will arrive in order! i.e.: You should expect one buffer to contain multiple messages, or just one, or the tail of a message and the head of another etc.
 * 3) The {@link PipedJacksonProtocolFilter#messageReceived(NextFilter, IoSession, Object)} receives an incoming buffer on a Session,
 * - If we have no {@link Reader} in the current Session, this is the first message and therefore prefixed by actual length of the message. We then create a new {@link Reader} linking up the IO and storing it in the ongoing session.
 * - If we have an existing Reader, we pass on the incoming buffer, read as much as necessary, signaling eventual finalization.
 * - If the buffer does not contain enough data to read a full Integer (i.e. 4 bytes) we spill those into a separate buffer to continue with the next buffer. (There is no handling for cases where the length might be part of more than 2 buffers because I am too lazy and that would be insanity).
 * - We repeat 3) until {@link IoBuffer#hasRemaining()} returns false, to respect the existence of multiple messages in a single buffer.
 * 4) If a Reader signals finalization we clear the Sessions reference to it, to force creation of a new one.
 * 5) The reader receives the {@link IoSession} and {@link org.apache.mina.core.filterchain.IoFilter.NextFilter} as parameters to ensure correct chainig. As a filter can be used by multiple sessions at once.
 */
@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PipedJacksonProtocolFilter extends IoFilterAdapter {
	private static final Thread.Builder THREAD_BUILDER = Thread.ofVirtual().name("%s#reader".formatted(PipedJacksonProtocolFilter.class.getSimpleName()), 0);

	private static final AttributeKey READER_KEY = new AttributeKey(PipedJacksonProtocolFilter.class, "reader");
	private static final AttributeKey SPILL_BUFFER = new AttributeKey(PipedJacksonProtocolFilter.class, "spill");

	/**
	 * Max size of a Buffer on my Windows machine was 64Kibi - to enable effective double buffering, we ... double it.
	 */
	private static final DataSize PIPED_IO_BUFFER_SIZE = DataSize.kibibytes(2 * 64);

	private final ObjectMapper mapper;
	private final int initialBufferLength;
	private final ExecutorService readerThreads = Executors.newCachedThreadPool((runnable) -> {
		Thread thread = THREAD_BUILDER.unstarted(runnable);
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
						// Buffer contains only partial length we need to combine with the next buffer.
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
	 * <p>
	 * Incoming buffers will combine their remaining part of the prefixed-length.
	 */
	private boolean requiresEdgeBuffering(IoSession session, IoBuffer buffer) throws IOException {
		if (buffer.remaining() >= Integer.BYTES) {
			return false;
		}

		assert session.getAttribute(SPILL_BUFFER) == null;

		log.trace("Not enough content in current buffer ({} bytes), spilling.", buffer.remaining());

		final IoBuffer spillBuffer = IoBuffer.allocate(Integer.BYTES, false);
		spillBuffer.setAutoExpand(false);

		buffer.asInputStream().transferTo(spillBuffer.asOutputStream());

		session.setAttribute(SPILL_BUFFER, spillBuffer);

		assert !buffer.hasRemaining();

		return true;
	}

	/**
	 * Reads the length of the incoming buffer.
	 * Incoming messages are always prefixed by their length.
	 *
	 * @implNote A single {@link IoBuffer} may contain multiple messages or parts of them.
	 * In very degenerate cases, the messages ends exactly such that the length of the next messages does not fully fit the current buffer and is spilled over into the next buffer.
	 * In that case we spill the remaining bytes into a separate buffer and append the incoming few bytes (at most 3!) into a separate buffer to calculate the length.
	 * This avoids costly copying and reallocation but looks really rather funky.
	 */
	private int getBufferLength(IoSession session, IoBuffer buffer) throws IOException {
		final IoBuffer spillBuffer;

		synchronized (session) {
			spillBuffer = (IoBuffer) session.setAttribute(SPILL_BUFFER, null);

			if (spillBuffer == null) {
				return buffer.getInt();
			}
		}

		log.trace("Found existing edge-buffer {}, incoming {}", DataSize.bytes(spillBuffer.position()), DataSize.bytes(buffer.limit()));

		buffer.getSlice(Integer.BYTES - spillBuffer.position()).asInputStream().transferTo(spillBuffer.asOutputStream());

		spillBuffer.flip();

		assert spillBuffer.remaining() == Integer.BYTES;

		return spillBuffer.getInt();
	}

	@NotNull
	private Reader newAsyncReader(int remaining, NextFilter nextFilter, IoSession session) {
		final Reader reader = new Reader(remaining, mapper, nextFilter, session, PIPED_IO_BUFFER_SIZE);

		readerThreads.submit(reader::read);

		session.setAttribute(READER_KEY, reader);
		return reader;
	}

	/**
	 * Serializes {@link WriteRequest#getMessage()} into an {@link IoBuffer} prefixed by the message's length.
	 */
	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
		//TODO use CachedBufferAllocator?
		final IoBuffer buf = IoBuffer.allocate(initialBufferLength, false);
		buf.setAutoExpand(true);

		final int initialPosition = buf.position(); // should always be 0

		buf.putInt(0); //DUMMY

		final Stopwatch stopwatch = Stopwatch.createStarted();
		log.trace("BEGIN Encoding message");

		final Object message = writeRequest.getMessage();
		try (OutputStream outputStream = buf.asOutputStream()) {
			mapper.writerFor(NetworkMessage.class).writeValue(outputStream, message);
		}

		log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), message);

		buf.putInt(initialPosition, buf.position() - initialPosition - Integer.BYTES);
		buf.flip();

		writeRequest.setMessage(buf);

		nextFilter.filterWrite(session, writeRequest);
	}

	/**
	 * Uses {@link PipedInputStream} / {@link PipedOutputStream} to do the actual reading of incoming values in a separate thread.
	 * <p>
	 * Messages may arrive in variable sized chunks therefore we need to track remaining data.
	 */
	@Data
	private class Reader {

		private final ObjectMapper mapper;
		private final PipedOutputStream outputStream;
		private final InputStream inputStream;
		private final NextFilter nextFilter;
		private final IoSession session;
		private int remaining;

		@SneakyThrows
		public Reader(int remaining, ObjectMapper mapper, NextFilter nextFilter, IoSession session, DataSize bufferSize) {
			this.mapper = mapper;
			this.nextFilter = nextFilter;
			this.session = session;
			this.remaining = remaining;
			outputStream = new PipedOutputStream();
			inputStream = new PipedInputStream(outputStream, ((int) bufferSize.toBytes()));
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
				// This thread can and will pause when inputStream does not have enough data.
				final NetworkMessage parsed = mapper.readValue(inputStream, NetworkMessage.class);
				nextFilter.messageReceived(session, parsed);
			}
			catch (IOException e) {
				log.error("Something went wrong reading from {}", session, e);
			}
		}
	}


}
