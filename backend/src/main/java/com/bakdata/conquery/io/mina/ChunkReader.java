package com.bakdata.conquery.io.mina;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.util.DebugMode;
import com.bakdata.conquery.util.io.EndCheckableInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.powerlibraries.io.Out;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
public class ChunkReader extends CumulativeProtocolDecoder {
	
	private static final AttributeKey MESSAGE_MANAGER = new AttributeKey(BinaryJacksonCoder.class, "messageManager");
	
	private final CQCoder<?> coder;
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		int pos = in.position();
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
			in.position(pos);
			return false;
		}

		MessageManager messageManager = getMessageManager(session);
		
		IoBuffer copy = IoBuffer.allocate(length);
		copy.put(in.array(), in.arrayOffset() + in.position(), length);
		copy.flip();
		in.skip(length);
		
		ChunkedMessage chunkedMessage = messageManager.getChunkedMessage(id);
		chunkedMessage.addBuffer(copy);

		if (last) {
			messageManager.remove(id);
			
			if(chunkedMessage.size() == 0) {
				throw new IllegalStateException("Received message of length 0");
			}
			
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
					if(DebugMode.isActive()) {
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
	public static class ChunkedMessage {

		private final UUID id;
		private final List<IoBuffer> buffers = new ArrayList<>();
		
		public EndCheckableInputStream createInputStream() {
			return new EndCheckableInputStream(JacksonUtil.stream(buffers));
		}

		public void addBuffer(IoBuffer copy) {
			buffers.add(copy);
		}

		public long size() {
			long size = 0;
			for(IoBuffer b:buffers) {
				size+=b.remaining();
			}
			return size;
		}

		@Override
		public String toString() {
			return "ChunkedMessage [id=" + id + ", buffers=" + buffers.stream().map(b->b.limit()+":"+b.getHexDump(15)).collect(Collectors.toList()) + "]";
		}
	}
	
	@Getter @RequiredArgsConstructor
	public static class MessageManager {
		
		private final Map<UUID, ChunkedMessage> messages = new HashMap<>();
		private UUID lastId = null;
		private ChunkedMessage lastMessage = null;
		
		public ChunkedMessage getChunkedMessage(UUID id) {
			if(id == lastId) {
				return lastMessage;
			}
			else {
				ChunkedMessage msg = messages.computeIfAbsent(id, ChunkedMessage::new);
				lastId = id;
				lastMessage = msg;
				return msg;
			}
		}

		public void remove(UUID id) {
			if(lastId == id) {
				lastId = null;
				lastMessage = null;
				messages.remove(id);
			}
		}

	}
}
