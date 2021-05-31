package com.bakdata.conquery.io.mina;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkReader extends CumulativeProtocolDecoder {
	
	private static final AttributeKey MESSAGE_MANAGER = new AttributeKey(BinaryJacksonCoder.class, "messageManager");
	
	private final CQCoder<?> coder;
	private final ObjectMapper mapper;
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		in.mark();
		if (in.remaining() < ChunkWriter.HEADER_SIZE) {
			return false;
		}

		boolean last = in.get() == ChunkWriter.LAST_MESSAGE;
		int length = in.getInt();
		if(length<0) {
			throw new IllegalStateException("Read message length "+length);
		}
		UUID id = new UUID(in.getLong(), in.getLong());
		
		if (in.remaining() < length) {
			in.reset();
			return false;
		}

		MessageManager messageManager = getMessageManager(session);
		
		if (last) {
			ChunkedMessage chunkedMessage = messageManager.finalBuffer(id, in, length);
			
			try {
				out.write(coder.decode(chunkedMessage));
			} catch (Exception e) {
				log.error(
						"Failed while deserializing the message {}: `{}` (Trying to create a dump as {}.json",
						chunkedMessage,
						JacksonUtil.toJsonDebug(chunkedMessage),
						id,
						e
				);

				dumpFailed(id, chunkedMessage.createInputStream());
			}
		}
		//if not the last part of the message we just store it
		else {
			messageManager.addBuffer(id, in, length);
		}

		return true;
	}

	private void dumpFailed(UUID id, InputStream inputStream) {
		Path dumps = Path.of("dumps");
		final File dumpFile = dumps.resolve("reading_" + id + "_" + Math.random() + ".json").toFile();

		try (InputStream is = inputStream) {
			Files.createDirectories(dumps);

			JsonNode tree = mapper.readTree(is);
			try(OutputStream os = new FileOutputStream(dumpFile)) {
				mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValue(os, tree);
			}
		} catch (Exception exception) {
			log.error("Failed to write the error json dump {}.json", id, exception);
		}
	}

	private MessageManager getMessageManager(IoSession session) {
		MessageManager messageManager = (MessageManager) session.getAttribute(MESSAGE_MANAGER);

		if (messageManager == null) {
			messageManager = new MessageManager();
			session.setAttribute(MESSAGE_MANAGER, messageManager);
		}
		return messageManager;
	}
	
	@Getter @RequiredArgsConstructor
	public static class MessageManager {
		
		private final Map<UUID, ChunkedMessage.List> messages = new HashMap<>();
		private UUID lastId = null;
		private ChunkedMessage.List lastMessage = null;
		
		public ChunkedMessage finalBuffer(UUID id, IoBuffer in, int length) {
			if(Objects.equals(lastId, id) || messages.containsKey(id)) {
				IoBuffer copy = IoBuffer.allocate(length);
				copy.put(in.array(), in.arrayOffset() + in.position(), length);
				copy.flip();
				in.skip(length);
				ChunkedMessage.List chunkedMessage = getChunkedMessage(id);
				remove(id);
				chunkedMessage.addBuffer(copy);
				return chunkedMessage;
			}
			return new ChunkedMessage.Singleton(in.getSlice(length));
		}

		public ChunkedMessage addBuffer(UUID id, IoBuffer in, int length) {
			IoBuffer copy = IoBuffer.allocate(length);
			copy.put(in.array(), in.arrayOffset() + in.position(), length);
			copy.flip();
			in.skip(length);
			ChunkedMessage.List chunkedMessage = getChunkedMessage(id);
			chunkedMessage.addBuffer(copy);
			return chunkedMessage;
		}

		private ChunkedMessage.List getChunkedMessage(UUID id) {
			if(id.equals(lastId)) {
				return lastMessage;
			}
			ChunkedMessage.List msg = messages.computeIfAbsent(id, a->new ChunkedMessage.List());
			lastId = id;
			lastMessage = msg;
			return msg;
		}

		private void remove(UUID id) {
			if(lastId == id) {
				lastId = null;
				lastMessage = null;
			}
			messages.remove(id);
		}

	}
}
