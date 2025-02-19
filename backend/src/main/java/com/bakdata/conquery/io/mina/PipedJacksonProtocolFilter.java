package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

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

	@ToString.Include
	private final String name;
	private final ObjectMapper mapper;

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
		final IoBuffer buffer = (IoBuffer) message;

		while (buffer.hasRemaining()) {
			synchronized (session) {
				Reader asyncReader = getAsyncReader(session);

				if (asyncReader == null) {
					final int remaining = buffer.getInt();
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
			}
		}
	}

	private Reader getAsyncReader(IoSession session) {
		return (Reader) session.getAttribute(READER_KEY);
	}

	@NotNull
	private Reader newAsyncReader(int remaining, NextFilter nextFilter, IoSession session) {
		final Reader reader = new Reader(remaining, mapper, nextFilter, session);
		reader.start();

		reader.setName("%s.%s/%s".formatted(getClass().getSimpleName(), name, session.toString()));
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
			mapper.writerFor(NetworkMessage.class)
				  .writeValue(outputStream, message);
		}

		log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), message);
		buf.putInt(0, buf.position() - Integer.BYTES);
		buf.flip();

		writeRequest.setMessage(buf);

		nextFilter.filterWrite(session, writeRequest);
	}

	@Data
	private class Reader extends Thread {

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
			inputStream = new PipedInputStream(outputStream, ((int) DataSize.megabytes(1).toBytes()));
			setDaemon(true);
		}

		public boolean receive(IoBuffer buffer) throws IOException {
			if (remaining > buffer.remaining()) {
				remaining -= buffer.remaining();

				try (InputStream inputStream = buffer.asInputStream()) {
					inputStream.transferTo(getOutputStream());
				}

				return false;
			}
			// else remaining <= buffer.remaining()

			final IoBuffer slice = buffer.getSlice(remaining);
			try (InputStream inputStream = slice.asInputStream()) {
				// This stupid method mutates BOTH buffer and slice WTF.
				final long written = inputStream.transferTo(getOutputStream());
				remaining -= (int) written;
			}

			getOutputStream().close();
			return true;
		}

		@Override
		public void run() {
			try {
				// It's important to not reuse the parsers!
				final NetworkMessage parsed = mapper.readValue(inputStream, NetworkMessage.class);
				log.trace("{}  Received {}", getName(), parsed);

				nextFilter.messageReceived(session, parsed);
			}
			catch (IOException e) {
				log.error("Something went wrong", e);
			}
		}
	}


}
