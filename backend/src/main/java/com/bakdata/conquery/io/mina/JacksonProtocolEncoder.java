package com.bakdata.conquery.io.mina;

import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;

@RequiredArgsConstructor
public class JacksonProtocolEncoder extends ObjectSerializationEncoder {

	private final ObjectWriter objectWriter;

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		IoBuffer buf = IoBuffer.allocate(64);
		buf.setAutoExpand(true);


		int oldPos = buf.position();
		buf.skip(4); // Make a room for the length field.

		objectWriter.writeValue(buf.asOutputStream(), message);

		int objectSize = buf.position() - 4;
		if (objectSize > getMaxObjectSize()) {
			throw new IllegalArgumentException("The encoded object is too big: " + objectSize + " (> " + getMaxObjectSize()
											   + ')');
		}

		// Fill the length field
		int newPos = buf.position();
		buf.position(oldPos);
		buf.putInt(newPos - oldPos - 4);
		buf.position(newPos);

		buf.flip();
		out.write(buf);
	}
}
