package com.bakdata.conquery.io.mina;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.core.JsonGenerator;
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
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

@Slf4j
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PipedJacksonProtocolFilter extends IoFilterAdapter {
	public static AtomicInteger instances = new AtomicInteger(0);

	@ToString.Include
	private final String name;
	private final ObjectMapper mapper;
	private final List<NetworkMessage> sent = new ArrayList<>();

	private final ConcurrentMap<IoSession, Reader> reader = new ConcurrentHashMap<>();


	@Override
	public void destroy() throws Exception {
		instances.decrementAndGet();
	}

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		Reader asyncReader = reader.computeIfAbsent(
				session,
				(ignored) -> {
					log.trace("Initializing decoder for {}/{}", nextFilter, name);

					instances.incrementAndGet();

					Reader reader = createReader(nextFilter, session, mapper);
					reader.setName("%s.%s/%s".formatted(getClass().getSimpleName(), name, session.toString()));
					return reader;
				}
		);

		final IoBuffer ioBuffer = (IoBuffer) message;

		while (((IoBuffer) message).hasRemaining()) {
			ioBuffer.asInputStream().transferTo(asyncReader.getOutputStream());
		}
	}


	@Override
	public synchronized void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
		final IoBuffer buf = IoBuffer.allocate(512, false);
		buf.setAutoExpand(true);

		try (JsonGenerator generator = mapper.getFactory().createGenerator(buf.asOutputStream())) {
			final Stopwatch stopwatch = Stopwatch.createStarted();
			log.trace("BEGIN Encoding message");

			final Object message = writeRequest.getMessage();
			generator.writeObject(message);
			log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), message);
		}

		buf.flip();

		writeRequest.setMessage(buf);
		nextFilter.filterWrite(session, writeRequest);
		sent.add(((NetworkMessage) writeRequest.getOriginalMessage()));
	}

	private Reader createReader(NextFilter nextFilter, IoSession session, ObjectMapper mapper) {
		final Reader readerThread = new Reader(mapper, nextFilter, session);
		readerThread.start();
		return readerThread;
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



		@SneakyThrows
		@Override
		public void run() {
			while (session.isActive()) {
				try {
					// It's important to not reuse the parsers!
					final NetworkMessage parsed = mapper.readValue(inputStream, NetworkMessage.class);
					received.add(parsed);

					if (!session.isActive()) {
						break;
					}

					nextFilter.messageReceived(session, parsed);
				}
				catch (IOException exception) {
					// Includes JacksonException
					log.error("Failed to decode message", exception);
				}
			}
			log.info("Session closed. Shutting down reader.");
			outputStream.close();
			inputStream.close();
		}
	}


}
