package com.bakdata.conquery.io.mina;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.BufferDataException;
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
			throw new IllegalStateException("Filter was added at the wrong place in the filterchain. Only expecting IoBuffers here. Got: " + writeRequest.getMessage());
		}

		try {
			if (ioBuffer.prefixedDataAvailable(4, socketSendBufferSize)) {
				// IoBuffer is shorter than socket buffer, we can just send it.
				log.trace("Sending buffer without chunking");
				super.filterWrite(nextFilter, session, writeRequest);
				return;
			}

			throw new IllegalArgumentException("Got an incomplete IoBuffer on the sender side.");
		}
		catch (BufferDataException e) {
			// The IoBuffer is larger than the socketSendBuffer
			log.trace("Sending buffer with chunking: {}", e.getMessage());
		}

		// Split buffers

		byte[] bufferArray = ioBuffer.array();
		int arrayOffset = ioBuffer.arrayOffset();

		int oldPos = ioBuffer.position();
		int oldLimit = ioBuffer.limit();


		ioBuffer.limit(oldPos + socketSendBufferSize);
		int newLimit = ioBuffer.limit();

		// Send the first resized (original) buffer, do not work on this ioBuffer pos and limit from here on
		int chunkCount = 1;
		log.trace("Sending {}. chunk: {} byte", chunkCount, newLimit - oldPos);
		DefaultWriteFuture future = new DefaultWriteFuture(session);
		nextFilter.filterWrite(session, new DefaultWriteRequest(ioBuffer, future));

		List<WriteFuture> futures = new ArrayList<>();
		futures.add(future);

		int remainingBytes = oldLimit - newLimit;

		do {
			int newArrayPos = arrayOffset + (newLimit - oldPos);

			// Size and allocate new Buffer
			int nextBufSize = Math.min(remainingBytes, socketSendBufferSize);
			IoBuffer nextBuffer = IoBuffer.allocate(nextBufSize);
			System.arraycopy(bufferArray, newArrayPos, nextBuffer.array(), nextBuffer.arrayOffset(), nextBufSize);

			// Write chunked buffer
			log.trace("Sending {}. chunk: {} byte", chunkCount, nextBufSize);
			future = new DefaultWriteFuture(session);
			futures.add(future);
			nextFilter.filterWrite(session, new DefaultWriteRequest(nextBuffer, future));

			// Recalculate for next iteration
			newLimit = newLimit + nextBufSize;
			remainingBytes = remainingBytes - nextBufSize;
			chunkCount++;

		} while(remainingBytes > 0);

		// Wait for our self produced write request to get through
		futures.forEach(WriteFuture::awaitUninterruptibly);

		// Set the original request as written
		writeRequest.getFuture().setWritten();
		ioBuffer.free();

		log.trace("Send all chunks");
	}
}
