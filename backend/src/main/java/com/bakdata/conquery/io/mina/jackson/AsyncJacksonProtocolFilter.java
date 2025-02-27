package com.bakdata.conquery.io.mina.jackson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.DataSize;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This {@link IoFilter} (de-)serializes POJOs using a Jackson {@link ObjectMapper}.
 * <p>
 * Since we can send very large messages we need to handle combination of multiple messages into one.
 * To avoid costly buffering in memory we instead pass incoming buffers to an asynchronous reader thread, which handles the actual reading.
 * <p>
 * In comparison {@link CumulativeProtocolDecoder} can end up at exponential runtime when messages become too large.
 * <p>
 * brief explanation of the reader implementation:
 * <p>
 * 1) {@link AsyncJacksonProtocolFilter#filterWrite(NextFilter, IoSession, WriteRequest)} serializes every message using the provided {@link ObjectMapper}, the length of the written contents is prefixed to the contents.
 * 2) This buffer is then transmitted by the Processor. There are virtually no guarantees about transmission except the buffers will arrive in order! i.e.: You should expect one buffer to contain multiple messages, or just one, or the tail of a message and the head of another etc.
 * 3) The {@link AsyncJacksonProtocolFilter#messageReceived(NextFilter, IoSession, Object)} receives an incoming buffer on a Session,
 * - If we have no {@link AsyncReader} in the current Session, this is the first message and therefore prefixed by actual length of the message. We then create a new {@link AsyncReader} linking up the IO and storing it in the ongoing session.
 * - If we have an existing Reader, we pass on the incoming buffer, read as much as necessary, signaling eventual finalization.
 * - In very rare cases: If the buffer does not contain enough data to read a full Integer (i.e. 4 bytes) we spill those into a separate buffer to continue with the next buffer. (There is no handling for cases where the length might be part of more than 2 buffers because I am too lazy and that would be insanity).
 * - We repeat 3) until {@link IoBuffer#hasRemaining()} returns false, to respect the existence of multiple messages in a single buffer.
 * 4) If a Reader signals finalization we clear the Sessions reference to it, to force creation of a new one.
 * 5) The reader receives the {@link IoSession} and {@link NextFilter} as parameters to ensure correct chainig. As a filter can be used by multiple sessions at once.
 */
@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class AsyncJacksonProtocolFilter extends IoFilterAdapter {

	/**
	 * Max size of a Buffer on my Windows machine was 64Kibi - to enable effective double buffering, we ... double it.
	 */
	private static final DataSize PIPED_IO_BUFFER_SIZE = DataSize.kibibytes(2 * 64);


	private static final Thread.Builder THREAD_BUILDER = Thread.ofVirtual().name("%s#reader".formatted(AsyncJacksonProtocolFilter.class.getSimpleName()), 0);

	private static final AttributeKey READER = new AttributeKey(AsyncJacksonProtocolFilter.class, "reader");
	private static final AttributeKey SPILL_BUFFER = new AttributeKey(AsyncJacksonProtocolFilter.class, "spill");
	private static final AttributeKey PUBLISHER = new AttributeKey(AsyncJacksonProtocolFilter.class, "publisher");


	private final ObjectMapper mapper;
	private final int initialBufferLength;

	private final ExecutorService readerThreads = Executors.newCachedThreadPool((runnable) -> {
		Thread thread = THREAD_BUILDER.unstarted(runnable);
		thread.setDaemon(true);

		return thread;
	});

	public AsyncJacksonProtocolFilter(ObjectMapper mapper, int initialBufferLength) {
		this.mapper = mapper;
		this.initialBufferLength = initialBufferLength;

	}

	@Override
	public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
		super.sessionOpened(nextFilter, session);

		ensureSessionPublisher(session);
	}

	private void ensureSessionPublisher(IoSession session) {
		if (session.containsAttribute(PUBLISHER)) {
			return;
		}

		log.debug("Creating new Publisher for {}", session);

		final AsyncPublisher publisher = new AsyncPublisher(session);

		final Thread publisherThread = new Thread(publisher::publish);

		publisherThread.setName("%s#publisher-%s".formatted(getClass().getSimpleName(), session));
		publisherThread.setDaemon(true);
		publisherThread.start();

		setSessionPublisher(session, publisher);
	}

	private static void setSessionPublisher(IoSession session, AsyncPublisher publisher) {
		session.setAttribute(PUBLISHER, publisher);
	}

	@Override
	public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
		super.sessionClosed(nextFilter, session);

		// close and discard the publisher.
		getSessionPublisher(session).close();
		setSessionPublisher(session, null);
	}

	private static AsyncPublisher getSessionPublisher(IoSession session) {
		return (AsyncPublisher) session.getAttribute(PUBLISHER);
	}

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		final IoBuffer buffer = (IoBuffer) message;

		log.trace("Received {}", buffer);

		synchronized (session) {
			do {
				log.trace("BEGIN handling {}", buffer);

				final AsyncReader asyncReader = getOrCreateAsyncReader(nextFilter, session, buffer);

				if (asyncReader == null) {
					// Rare spill-buffer case
					break;
				}

				asyncReader.receive(buffer);

				if (asyncReader.hasEnough()) {
					setSessionReader(session, null);
				}

				log.trace("DONE handling {}, {} remaining", buffer, buffer.remaining());
			} while (buffer.hasRemaining());
		}
	}

	@Nullable
	private AsyncReader getOrCreateAsyncReader(NextFilter nextFilter, IoSession session, IoBuffer buffer) throws IOException {
		final AsyncReader asyncReader = getSessionReader(session);

		if (asyncReader != null) {
			return asyncReader;
		}

		if (requiresSpillBuffer(buffer)) {
			final IoBuffer spillBuffer = createSpillBuffer(buffer);

			final IoBuffer priorSpill = setSessionSpillBuffer(session, spillBuffer);
			assert priorSpill == null;

			// Buffer contains only partial length we need to combine with the next buffer.
			return null;
		}

		final int bufferLength = calculateMessageLength(session, buffer);

		final AsyncReader reader = newAsyncReader(bufferLength, nextFilter, session);

		setSessionReader(session, reader);

		return reader;
	}

	private static void setSessionReader(IoSession session, AsyncReader reader) {
		session.setAttribute(READER, reader);
	}

	private static AsyncReader getSessionReader(IoSession session) {
		return (AsyncReader) session.getAttribute(READER);
	}

	/**
	 * Test if we have enough contents to read the incoming buffer's length.
	 * If not, cumulate the remaining buffer into a new buffer.
	 * <p>
	 * Incoming buffers will combine their remaining part of the prefixed-length.
	 */
	private boolean requiresSpillBuffer(IoBuffer buffer) {
		return buffer.remaining() < Integer.BYTES;
	}

	@NotNull
	private static IoBuffer createSpillBuffer(IoBuffer buffer) throws IOException {
		log.trace("Not enough content in current buffer ({} bytes), spilling.", buffer.remaining());

		final IoBuffer spillBuffer = IoBuffer.allocate(Integer.BYTES, false);
		spillBuffer.setAutoExpand(false);

		buffer.asInputStream().transferTo(spillBuffer.asOutputStream());
		return spillBuffer;
	}

	private static IoBuffer setSessionSpillBuffer(IoSession session, IoBuffer spillBuffer) {
		return (IoBuffer) session.setAttribute(SPILL_BUFFER, spillBuffer);
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
	private int calculateMessageLength(IoSession session, IoBuffer buffer) throws IOException {

		if (!sessionHasSpillBuffer(session)) {
			return buffer.getInt();
		}

		final IoBuffer spillBuffer = setSessionSpillBuffer(session, null);

		log.trace("Found existing spill-buffer {}, incoming {}", DataSize.bytes(spillBuffer.position()), DataSize.bytes(buffer.limit()));

		buffer.getSlice(Integer.BYTES - spillBuffer.position()).asInputStream().transferTo(spillBuffer.asOutputStream());

		spillBuffer.flip();

		return spillBuffer.getInt();
	}

	@NotNull
	private AsyncReader newAsyncReader(int bufferLength, NextFilter nextFilter, IoSession session) {
		log.trace("Creating new reader for {}", DataSize.bytes(bufferLength));

		final AsyncReader reader = new AsyncReader(bufferLength, mapper, nextFilter, PIPED_IO_BUFFER_SIZE);

		// Enqueue the reader so that it can be bubbled up in order.
		getSessionPublisher(session).register(reader);

		readerThreads.submit(reader::read);
		return reader;
	}

	private static boolean sessionHasSpillBuffer(IoSession session) {
		return session.containsAttribute(SPILL_BUFFER);
	}

	/**
	 * Serializes {@link WriteRequest#getMessage()} into an {@link IoBuffer} prefixed by the message's length.
	 */
	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
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


}
