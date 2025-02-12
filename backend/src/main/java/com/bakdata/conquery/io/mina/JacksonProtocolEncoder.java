package com.bakdata.conquery.io.mina;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Stopwatch;
import io.dropwizard.util.DataSize;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;

@Slf4j
@RequiredArgsConstructor
public class JacksonProtocolEncoder extends ObjectSerializationEncoder {

	private final int SIZE_PREFIX_LENGTH = Integer.BYTES;

	private final ObjectWriter objectWriter;

	@Getter
	@Setter
	private int initialBufferCapacityBytes = 64;

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		if (!(message instanceof NetworkMessage<?> networkMessage)) {
			throw new IllegalArgumentException("Message must be of type %s (was %s)".formatted(NetworkMessage.class, message.getClass()));
		}

		final IoBuffer buf = IoBuffer.allocate(initialBufferCapacityBytes, false);
		buf.setAutoExpand(true);

		buf.skip(SIZE_PREFIX_LENGTH); // Make a room for the length field.

		final Stopwatch stopwatch = Stopwatch.createStarted();
		log.trace("BEGIN Encoding message: {}", networkMessage);

		objectWriter.writeValue(buf.asOutputStream(), networkMessage);

		final int objectSize = buf.position() - SIZE_PREFIX_LENGTH;

		if (objectSize > getMaxObjectSize()) {
			throw new IllegalArgumentException("The encoded object is too big: " + objectSize + " (> " + getMaxObjectSize() + ')');
		}

		// Fill the length field
		buf.putInt(0, objectSize);

		buf.flip();
		log.trace("FINISHED Encoding message in {}. Buffer size: {}. Message: {}", stopwatch, DataSize.bytes(buf.remaining()), networkMessage);

		out.write(buf);
	}
}
