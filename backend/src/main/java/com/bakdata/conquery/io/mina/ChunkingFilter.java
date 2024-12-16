package com.bakdata.conquery.io.mina;

import java.util.concurrent.atomic.AtomicInteger;

import io.dropwizard.util.DataSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.jetbrains.annotations.NotNull;

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
		final int totalSize = ioBuffer.remaining();

		// TODO unsure if Atomic is needed here
		final AtomicInteger writtenChunks = new AtomicInteger();
		final int totalChunks = divideAndRoundUp(totalSize, socketSendBufferSize);

		// Send the first resized (original) buffer
		int chunkCount = 0;

		IoFutureListener<IoFuture> listener = handleWrittenChunk(writeRequest, writtenChunks, totalChunks);

		DefaultWriteFuture future;

		int position = ioBuffer.position();
		int remainingBytes = totalSize;

		do {
			// Size a new Buffer
			int nextBufSize = Math.min(remainingBytes, socketSendBufferSize);
			IoBuffer slice = ioBuffer.getSlice(position, nextBufSize);

			// Write chunked buffer
			chunkCount++;
			log.trace("Sending {}. chunk: {} byte", chunkCount, nextBufSize);
			future = new DefaultWriteFuture(session);

			nextFilter.filterWrite(session, new DefaultWriteRequest(slice, future));

			future.addListener(listener);

			// Recalculate for next iteration
			position += nextBufSize;
			remainingBytes -= nextBufSize;

		} while(remainingBytes > 0);
	}

	@NotNull
	private static IoFutureListener<IoFuture> handleWrittenChunk(WriteRequest writeRequest, AtomicInteger writtenChunks, int totalChunks) {
		return f -> {
			// Count written chunk and notify original writeRequest on error or success

			WriteFuture chunkFuture = (WriteFuture) f;
			WriteFuture originalFuture = writeRequest.getFuture();
			if (!chunkFuture.isWritten()) {
				log.warn("Failed to write chunk");
				if (!originalFuture.isDone()) {
					originalFuture.setException(new IllegalStateException("Failed to write a chunked ioBuffer", chunkFuture.getException()));
				}
				return;
			}
			int writtenChunk = writtenChunks.incrementAndGet();
			if (writtenChunk >= totalChunks) {
				log.trace("Sent all {} chunks", writtenChunk);
				originalFuture.setWritten();
			}
		};
	}

	public static int divideAndRoundUp(int num, int divisor) {
		// only for positive values
		return (num + divisor - 1) / divisor;
	}
}
