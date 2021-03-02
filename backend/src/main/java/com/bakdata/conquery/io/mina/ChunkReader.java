package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
	private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

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

		MessageManager messageManager = getMessageManager(session, service);
		try {
			messageManager.addBuffer(id, in, length);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		if (last) {

			try {
				ChunkedMessage chunkedMessage = messageManager.finish(id);
				out.write(coder.decode(chunkedMessage));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				messageManager.remove(id);
			}
		}


		return true;
	}

	private MessageManager getMessageManager(IoSession session, ListeningExecutorService service) {
		MessageManager messageManager = (MessageManager) session.getAttribute(MESSAGE_MANAGER);

		if (messageManager == null) {
			messageManager = new MessageManager(service, this.coder);
			session.setAttribute(MESSAGE_MANAGER, messageManager);
		}
		return messageManager;
	}

	@Getter
	@RequiredArgsConstructor
	public static class MessageManager {

		private final ListeningExecutorService service;
		private final BinaryJacksonCoder coder;

		private final Map<UUID, ChunkedMessage> messages = new HashMap<>();

		public ChunkedMessage addBuffer(UUID id, IoBuffer in, int length) throws IOException {
			IoBuffer copy = IoBuffer.allocate(length);
			copy.put(in.array(), in.arrayOffset() + in.position(), length);
			copy.flip();
			in.skip(length);
			ChunkedMessage chunkedMessage = getChunkedMessage(id);
			chunkedMessage.addBuffer(copy, length);


			return chunkedMessage;
		}

		private ChunkedMessage getChunkedMessage(UUID id) {
			return messages.computeIfAbsent(id, (ignored) -> {
				try {
					return new ChunkedMessage(service, coder.getReader());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			});
		}

		private void remove(UUID id) {
			messages.remove(id);
		}

		public ChunkedMessage finish(UUID id) throws IOException {
			final ChunkedMessage message = getChunkedMessage(id);
			message.getOutputStream().flush();
			message.getOutputStream().close();
			return message;
		}
	}
}
