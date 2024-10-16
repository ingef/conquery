package com.bakdata.conquery.io.mina;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.util.io.EndCheckableInputStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.buffer.IoBuffer;

public interface ChunkedMessage {
	
	long size();
	EndCheckableInputStream createInputStream();
	
	@Getter @RequiredArgsConstructor
	class Singleton implements ChunkedMessage {
	
		private final IoBuffer buffer;
		
		@Override
		public EndCheckableInputStream createInputStream() {
			return new EndCheckableInputStream(JacksonUtil.stream(buffer));
		}
	
		@Override
		public long size() {
			return buffer.remaining();
		}
	
		@Override
		public String toString() {
			return "ChunkedMessage [buffers=" + buffer.limit() + "]";
		}
	}
	
	@Getter @RequiredArgsConstructor
	class List implements ChunkedMessage {
	
		private final java.util.List<IoBuffer> buffers = new ArrayList<>();
		
		@Override
		public EndCheckableInputStream createInputStream() {
			return new EndCheckableInputStream(JacksonUtil.stream(buffers));
		}
	
		public void addBuffer(IoBuffer copy) {
			buffers.add(copy);
		}
	
		@Override
		public long size() {
			long size = 0;
			for(IoBuffer b:buffers) {
				size+=b.remaining();
			}
			return size;
		}
	
		@Override
		public String toString() {
			return "ChunkedMessage [buffers=" + buffers.stream().map(b->b.limit()).collect(Collectors.toList()) + "]";
		}
	}
}