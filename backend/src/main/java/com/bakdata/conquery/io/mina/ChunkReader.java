package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j
@RequiredArgsConstructor
public class ChunkReader extends CumulativeProtocolDecoder {

	private static final AttributeKey MESSAGE_MANAGER = new AttributeKey(BinaryJacksonCoder.class, "messageManager");

	private final BinaryJacksonCoder coder;

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		in.mark();
		if (in.remaining() < ChunkWriter.HEADER_SIZE) {
			return false;
		}

		boolean last = in.get() == ChunkWriter.LAST_MESSAGE;
		int length = in.getInt();

		if (length < 0) {
			throw new IllegalStateException("Read message length " + length);
		}

		UUID id = new UUID(in.getLong(), in.getLong());

		if (in.remaining() < length) {
			in.reset();
			return false;
		}

		MessageManager messageManager = getMessageManager(session);

		try {
			final ChunkedMessage chunkedMessage = messageManager.get(id);
			final ByteArrayFeeder feeder = chunkedMessage.getFeeder();

			// TODO this is just for testing
			while (!feeder.needMoreInput()){
				Thread.sleep(100);
			}

			feeder.feedInput(in.array(), in.arrayOffset() + in.position(), in.position() + length);
			in.skip(length);

			if (last) {
				feeder.endOfInput();
				out.write(coder.decode(chunkedMessage));
				messageManager.remove(id);
			}
		}
		catch (Exception e) {
			log.error("Failed parsing Message[{}]", id, e);
		}

		return true;
	}

	private MessageManager getMessageManager(IoSession session) {
		MessageManager messageManager = (MessageManager) session.getAttribute(MESSAGE_MANAGER);

		if (messageManager == null) {
			messageManager = new MessageManager(coder);
			session.setAttribute(MESSAGE_MANAGER, messageManager);
		}
		return messageManager;
	}

	@Getter
	@RequiredArgsConstructor
	public static class MessageManager {
		private final BinaryJacksonCoder coder;

		private final Map<UUID, ChunkedMessage> messages = new HashMap<>();

		private ChunkedMessage get(UUID id) throws IOException {
			if (messages.containsKey(id)) {
				return messages.get(id);
			}

			final ChunkedMessage message = createMessage();
			messages.put(id, message);
			return message;
		}

		private ChunkedMessage createMessage() throws IOException {
			final JsonParser parser = coder.getReader().getFactory().createNonBlockingByteArrayParser();

			return new ChunkedMessage((ByteArrayFeeder) parser.getNonBlockingInputFeeder(), parser);
		}

		public void remove(UUID id) throws IOException {
			final ChunkedMessage message = messages.remove(id);
			message.getParser().close();
		}
	}
}
