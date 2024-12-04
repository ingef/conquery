package com.bakdata.conquery.io.mina;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j
@RequiredArgsConstructor
public class JacksonProtocolDecoder extends CumulativeProtocolDecoder {

	private final ObjectReader objectReader;

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		if (!in.prefixedDataAvailable(4, Integer.MAX_VALUE)) {
			// Not enough data available, cumulate more
			return false;
		}

		int length = in.getInt();
		if (length <= 4) {
			throw new BufferDataException("Object length should be greater than 4: " + length);
		}

		// Resize limit to the frame only the object that we want to read now
		int oldLimit = in.limit();
		int beforeReadPosition = in.position();
		in.limit(in.position() + length);
		int objectEndLimit = in.limit();

		try {
			// Read the object we are interested in
			Object o = objectReader.readValue(in.asInputStream());
			out.write(o);
		}
		catch (IOException e) {
			String debuggedMessage = "enable TRACE for Message";
			if (log.isTraceEnabled()) {
				// Rewind ordinary read attempt
				in.position(beforeReadPosition);

				debuggedMessage = JacksonUtil.toJsonDebug(in.asInputStream());

				// If for some reason the debugging decoder did not read all bytes: forward the position to this object's supposed end
				in.position(objectEndLimit);
			}
			log.error("Failed to decode message: {}", debuggedMessage , e);
		}
		finally {
			// Set back the old limit, as the in buffer might already have data for a new object
			in.limit(oldLimit);
		}

		return true;
	}
}
