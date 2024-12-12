package com.bakdata.conquery.io.mina;

import java.util.ArrayList;
import java.util.List;

import io.dropwizard.util.DataSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

/**
 * Chunks messages to fit them in the socket send buffer size, iff they are larger than the socket send buffer.
 * The given ioBuffer is simply sliced into smaller ioBuffers up to the size of {@link ChunkingFilter#socketSendBufferSize}.
 */
@RequiredArgsConstructor
@Slf4j
public class ChunkingFilter extends IoFilterAdapter {

	private final int socketSendBufferSize;




	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
		if (!(writeRequest.getMessage() instanceof IoBuffer ioBuffer)) {
			throw new IllegalStateException("Filter was added at the wrong place in the filter chain. Only expecting IoBuffers here. Got: " + writeRequest.getMessage());
		}

		// The first 4 bytes hold the object length in bytes
		int objectLength = ioBuffer.getInt(ioBuffer.position());


		if (objectLength < socketSendBufferSize) {
			// IoBuffer is shorter than socket buffer, we can just send it.
			log.trace("Sending buffer without chunking: {} (limit = {})", DataSize.bytes(objectLength), DataSize.bytes(socketSendBufferSize));
			super.filterWrite(nextFilter, session, writeRequest);
			return;
		}

		// Split buffers
		int oldPos = ioBuffer.position();
		int oldLimit = ioBuffer.limit();


		ioBuffer.limit(oldPos + socketSendBufferSize);
		int newLimit = ioBuffer.limit();

		// Send the first resized (original) buffer
		int chunkCount = 1;
		log.trace("Sending {}. chunk: {} byte", chunkCount, ioBuffer.remaining());
		DefaultWriteFuture future = new DefaultWriteFuture(session);
		nextFilter.filterWrite(session, new DefaultWriteRequest(ioBuffer, future));

		List<WriteFuture> futures = new ArrayList<>();
		futures.add(future);

		int remainingBytes = oldLimit - newLimit;

		do {
			// Size a new Buffer
			int nextBufSize = Math.min(remainingBytes, socketSendBufferSize);
			IoBuffer nextBuffer = ioBuffer.duplicate();
			nextBuffer.limit(newLimit + nextBufSize);
			nextBuffer.position(newLimit);

			// Write chunked buffer
			chunkCount++;
			log.trace("Sending {}. chunk: {} byte", chunkCount, nextBufSize);
			future = new DefaultWriteFuture(session);
			futures.add(future);
			nextFilter.filterWrite(session, new DefaultWriteRequest(nextBuffer, future));

			// Recalculate for next iteration
			newLimit = newLimit + nextBufSize;
			remainingBytes = remainingBytes - nextBufSize;

		} while(remainingBytes > 0);

		try {
			// Wait for our self produced write request to get through
			futures.forEach(WriteFuture::awaitUninterruptibly);

			for (WriteFuture writeFuture : futures) {
				Throwable exception = writeFuture.getException();
				if (exception == null) {
					continue;
				}

				log.warn("Failed to send all {} chunks", chunkCount, exception);
				writeRequest.getFuture().setException(new IllegalStateException("Failed to write a chunked ioBuffer", exception));
				break;
			}


			log.trace("Sent all {} chunks", chunkCount);
			// Set the original request as written
			writeRequest.getFuture().setWritten();
		}
		finally {
			ioBuffer.free();
		}
	}
}
