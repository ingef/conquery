package com.bakdata.conquery.io.mina;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.powerlibraries.io.Out;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkReader extends CumulativeProtocolDecoder {
	
	private static final AttributeKey MESSAGE_MANAGER = new AttributeKey(BinaryJacksonCoder.class, "messageManager");
	
	private final CQCoder<?> coder;
	
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
				log.error("Failed while deserializing the message "
						+ chunkedMessage
						+ ":'"
						+ JacksonUtil.toJsonDebug(chunkedMessage)
						+ "'.\n\tI tried to create a dump as "
						+ id
						+ ".json"
					, e
				);
				
				try (InputStream is = chunkedMessage.createInputStream()) {
					JsonNode tree = Jackson.BINARY_MAPPER.readTree(is);
					try(OutputStream os = Out.file("dumps/reading_"+id+"_"+Math.random()+".json").asStream()) {
						Jackson.MAPPER.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValue(os, tree);
					}
				} catch (Exception e1) {
					log.error("Failed to write the error json dump "+id+".json, trying as bin", e1);
					if(log.isTraceEnabled()) {
						try (InputStream is = chunkedMessage.createInputStream()) {
							File dumps = new File("dumps");
							dumps.mkdirs();
							try(OutputStream os = Out.file(dumps, "reading_"+id+"_"+Math.random()+".bin").asStream()) {
								IOUtils.copy(is, os);
							}
						} catch (Exception e2) {
							log.error("Failed to write the error json dump "+id+".bin", e2);
						}
					}
				}
			}
		}
		//if not the last part of the message we just store it
		else {
			messageManager.addBuffer(id, in, length);
		}

		return true;
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
