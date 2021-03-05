package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.util.SimplePool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkWriter extends ProtocolEncoderAdapter {

	public static final int HEADER_SIZE = Integer.BYTES + Byte.BYTES + 2*Long.BYTES;
	public static final byte LAST_MESSAGE = 1;
	public static final byte CONTINUED_MESSAGE = 0;

	private final BinaryJacksonCoder coder;

	@Getter
	private final int bufferSizeBytes;
	private final SimplePool<IoBuffer> bufferPool = new SimplePool<>(this::allocateBuffer);

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		Chunkable ch = coder.encode(((NetworkMessage<?>) message));
		try(ChunkOutputStream cos = new ChunkOutputStream(ch.getId(), out)) {
			ch.writeMessage(cos);
		}
	}

	private IoBuffer allocateBuffer() {
		return IoBuffer.allocate(getBufferSizeBytes());
	}

	@RequiredArgsConstructor
	private class ChunkOutputStream extends OutputStream {
		private final UUID id;
		private final ProtocolEncoderOutput out;
		private IoBuffer buffer = null;
		private boolean closed = false;
		private int writtenBytes = 0;
		
		private void newBuffer(int required) {
			if(buffer == null || buffer.remaining()<required) {
				if(buffer != null) {
					finishBuffer(false);
				}
				buffer = bufferPool.borrow();
				buffer.position(HEADER_SIZE);
			}
		}

		private void finishBuffer(boolean end) {
			buffer.flip();
			if(buffer.remaining() - HEADER_SIZE == 0) {
				throw new IllegalStateException();
			}
			buffer.put(0, end ? LAST_MESSAGE : CONTINUED_MESSAGE);
			buffer.putInt(Byte.BYTES, buffer.remaining() - HEADER_SIZE);
			buffer.putLong(Byte.BYTES+Integer.BYTES, id.getMostSignificantBits());
			buffer.putLong(Byte.BYTES+Integer.BYTES+Long.BYTES, id.getLeastSignificantBits());
			out.write(buffer);
			final IoBuffer currentBuffer = buffer;
			out.flush().addListener(future-> {
				currentBuffer.clear();
				bufferPool.returnValue(currentBuffer);
			});
			buffer = null;
		}

		@Override
		public void write(int b) throws IOException {
			if(closed) {
				throw new IllegalStateException();
			}
			newBuffer(1);
			buffer.put((byte)b);
			writtenBytes++;
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if(closed) {
				throw new IllegalStateException();
			}
			if (b == null) {
				throw new NullPointerException();
			} else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
				throw new IndexOutOfBoundsException();
			} else if (len == 0) {
				return;
			}

			writtenBytes += len;

			while(len > 0) {
				if(buffer == null || !buffer.hasRemaining()) {
					newBuffer(len);
				}
				
				int write = Math.min(len, buffer.remaining());
				buffer.put(b, off, write);
				len -= write;
				off += write;
			}
		}
		
		@Override
		public void close() throws IOException {
			if(!closed) {
				newBuffer(0);
				finishBuffer(true);
				closed = true;

				log.trace("Message[{}] was of size {} byte", id, writtenBytes);
			}
		}
	}
}