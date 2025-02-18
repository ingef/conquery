package com.bakdata.conquery.io.mina;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.DataSize;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PipedJacksonProtocolFilter extends IoFilterAdapter {
	private static final AttributeKey READER_KEY = new AttributeKey(PipedJacksonProtocolFilter.class, "reader");
	private static final AttributeKey WRITER_WRITTEN_KEY = new AttributeKey(PipedJacksonProtocolFilter.class, "sent");
	public static AtomicInteger instances = new AtomicInteger(0);
	@ToString.Include
	private final String name;
	private final ObjectMapper mapper;

	@Override
	public void destroy() throws Exception {
		instances.decrementAndGet();
	}

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		synchronized (session) {
			Reader asyncReader = getAsyncReader(nextFilter, session);

			final IoBuffer ioBuffer = (IoBuffer) message;

			while (((IoBuffer) message).hasRemaining()) {
				try (InputStream inputStream = ioBuffer.asInputStream()) {
					inputStream.transferTo(asyncReader.getOutputStream());
				}
			}
		}
	}

	private Reader getAsyncReader(NextFilter nextFilter, IoSession session) {
		if (session.getAttribute(READER_KEY) != null) {
			return (Reader) session.getAttribute(READER_KEY);
		}

		log.trace("Initializing decoder for {}/{}", nextFilter, name);

		instances.incrementAndGet();

		final Reader reader = createReader(nextFilter, session, mapper);
		reader.setName("%s.%s/%s".formatted(getClass().getSimpleName(), name, session.toString()));
		session.setAttribute(READER_KEY, reader);

		return reader;

	}

	private Reader createReader(NextFilter nextFilter, IoSession session, ObjectMapper mapper) {
		final Reader readerThread = new Reader(mapper, nextFilter, session);
		readerThread.start();
		return readerThread;
	}

	@Override
	public synchronized void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
		//TODO use CachedBufferAllocator?
		final IoBuffer buf = IoBuffer.allocate(512, false);
		buf.setAutoExpand(true);

		final Stopwatch stopwatch = Stopwatch.createStarted();
		log.trace("BEGIN Encoding message");

		final Object message = writeRequest.getMessage();
		try (OutputStream outputStream = buf.asOutputStream()) {
			mapper.writerFor(NetworkMessage.class)
				  .writeValue(outputStream, message);
		}

		log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), message);

		buf.flip();

		writeRequest.setMessage(buf);

		recordSent(session, ((NetworkMessage) message));

		nextFilter.filterWrite(session, writeRequest);
	}

	private void recordSent(IoSession session, NetworkMessage message) {
		log.info("Sent message {} to {}", message, session);
		((List<NetworkMessage>) session.getAttribute(WRITER_WRITTEN_KEY, new ArrayList<>())).add(message);
	}

	@Data
	private class Reader extends Thread {

		private final ObjectMapper mapper;
		private final PipedOutputStream outputStream;
		private final InputStream inputStream;
		private final NextFilter nextFilter;
		private final IoSession session;

		private final AtomicBoolean running = new AtomicBoolean(true);
		private final List<NetworkMessage> received = new ArrayList<>();

		@SneakyThrows
		public Reader(ObjectMapper mapper, NextFilter nextFilter, IoSession session) {
			this.mapper = mapper;
			this.nextFilter = nextFilter;
			this.session = session;
			outputStream = new PipedOutputStream();
			inputStream = new PipedInputStream(outputStream, ((int) DataSize.megabytes(1).toBytes()));
			setDaemon(true);
		}


		@Override
		public void run() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TeeInputStream inputStream = new TeeInputStream(this.inputStream, baos);

			while (session.isActive()) {
				try {
					// It's important to not reuse the parsers!
					final NetworkMessage parsed = mapper.readValue(inputStream, NetworkMessage.class);
					log.info("{}  Received {}", getName(), parsed);
					received.add(parsed);

					if (!session.isActive()) {
						break;
					}

					nextFilter.messageReceived(session, parsed);
				}
				catch (JsonMappingException jse) {
					log.error("Failed to decode message", jse);
				}
				catch (IOException exception) {
					// Includes JacksonException
					log.error("Failed to decode message", exception);
				}
				catch (Exception e) {
					log.error("Something went wrong", e);
				}
			}

			try {
				log.info("Session closed. Shutting down reader.");
				outputStream.close();
				inputStream.close();
			}
			catch (IOException e) {
				log.error("Failed to close streams", e);
			}
		}
	}


}
