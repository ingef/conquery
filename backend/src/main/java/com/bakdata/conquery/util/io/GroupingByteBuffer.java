package com.bakdata.conquery.util.io;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import com.google.common.primitives.Ints;

import io.dropwizard.util.Size;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GroupingByteBuffer implements Closeable {
	
	private ByteBuffer buffer = ByteBuffer.allocate(Ints.checkedCast(Size.megabytes(64).toBytes()));
	private final Consumer<byte[]> bufferConsumer;
	
	public void ensureCapacity(int length) {
		//in case the data would fit we have no problem
		if(buffer.remaining()<length) {
			
			//in case the buffer is empty but the data is larger we size it up to fit
			if(isEmpty()) {
				buffer = ByteBuffer.allocate(length);
			}
			//otherwise we just need to flip the buffer to make space
			else {
				flip();
				ensureCapacity(length);
			}
		}
	}
	
	public void clear() {
		buffer.clear();
	}

	public byte[] internalArray() {
		return buffer.array();
	}

	public int offset() {
		return buffer.arrayOffset()+buffer.position();
	}

	public boolean isEmpty() {
		return buffer.position()==0;
	}

	public ByteBuffer putInt(int value) {
		return buffer.putInt(value);
	}

	public ByteBuffer putLong(long value) {
		return buffer.putLong(value);
	}

	public void advance(int size) {
		buffer.position(buffer.position()+size);
	}

	@Override
	public void close() {
		flip();
	}

	private void flip() {
		if(!isEmpty()) {
			byte[] bytes = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.position());
			bufferConsumer.accept(bytes);
			buffer.clear();
		}
	}
}
