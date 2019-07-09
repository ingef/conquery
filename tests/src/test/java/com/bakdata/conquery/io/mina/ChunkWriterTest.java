package com.bakdata.conquery.io.mina;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.mina.ChunkReader.ChunkedMessage;
import com.bakdata.conquery.util.io.EndCheckableInputStream;


public class ChunkWriterTest {
	
	public static Collection<Integer> test() {
		List<Integer> l = new ArrayList<>();
		for(int i=1; i<21; i++) {
			l.add(i);
		}
		return l;
	}
	
	@ParameterizedTest
	@MethodSource
	public void test(int messageLength) throws Exception {
		Random r = new Random(7);
		byte[] bytes = new byte[messageLength];
		UUID id = UUID.nameUUIDFromBytes(bytes);
		r.nextBytes(bytes);
		
		ChunkWriter chunker = new ChunkWriter(new CQCoder<byte[]>() {
			@Override
			public byte[] decode(ChunkedMessage message) throws Exception {
				return fail();
			}
			
			@Override
			public Chunkable encode(byte[] message) throws Exception {
				return new Chunkable(id, null, message) {
					@Override
					public void writeMessage(OutputStream out) {
						try {
							out.write(bytes, 0, bytes.length);
						} catch(Exception e) {
							fail(e);
						}
					}
				};
			}
		});
		
		chunker.setBufferSize(26);
		AbstractProtocolEncoderOutput out = new AbstractProtocolEncoderOutput() {
			@Override
			public WriteFuture flush() {
				return DefaultWriteFuture.newWrittenFuture(null);
			}
		};
		
		try(EndCheckableInputStream is = new EndCheckableInputStream(new ByteArrayInputStream(bytes))) {
			chunker.encode(null, bytes, out);
		}
		
		List<IoBuffer> buffers = out.getMessageQueue()
				.stream()
				.map(IoBuffer.class::cast)
				.collect(Collectors.toList());
		
		assertThat(bytes).hasSize(messageLength);
		assertThat(buffers).hasSize((messageLength-1)/5+1);
		assertThat(buffers).allSatisfy(b -> {
			assertThat(b.limit()).isGreaterThan(5);
			assertThat(b.getInt(1)).isEqualTo(b.limit()-21);
		});
		assertThat(buffers.subList(0, buffers.size()-1)).allSatisfy(b -> assertThat(b.get(0)).isEqualTo(ChunkWriter.CONTINUED_MESSAGE));
		assertThat(buffers.get(buffers.size()-1)).satisfies(b -> assertThat(b.get(0)).as("last message is marked").isEqualTo(ChunkWriter.LAST_MESSAGE));
	}
}
