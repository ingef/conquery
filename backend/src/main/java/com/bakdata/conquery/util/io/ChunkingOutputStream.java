package com.bakdata.conquery.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public class ChunkingOutputStream extends OutputStream {
	private byte[] buf;
	private int off = 0;
	private final int size;
	private final Consumer<byte[]> consumer;

	public ChunkingOutputStream(int size, Consumer<byte[]> consumer) {
		this.size = size;
		this.consumer = consumer;
		buf = new byte[size];
	}

	@Override
	public void write(int b) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(byte[] buf, int off, int len) throws IOException {
		if ((off < 0) || (off > buf.length) || (len < 0) || ((off + len) > buf.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		}
		while (len > 0) {
			allocateBufferIfNeeded();
			int effectiveLen = Math.min(len, size - this.off);
			System.arraycopy(buf, off, this.buf, this.off, effectiveLen);
			this.off += effectiveLen;
			off += effectiveLen;
			len -= effectiveLen;
			flush();
		}
	}

	private void allocateBufferIfNeeded() {
		if (buf == null) {
			buf = new byte[size];
			off = 0;
		}
	}

	@Override
	public void flush() throws IOException {
		if (off == size) {
			write();
		}
	}

	@Override
	public void close() throws IOException {
		write();
	}
	
	private void write() {
		if (off > 0) {
			if(off == size) {
				consumer.accept(buf);
				buf = null;
			}
			else {
				consumer.accept(Arrays.copyOf(buf, off));
			}
			off = 0;
			
		}
	}
}
