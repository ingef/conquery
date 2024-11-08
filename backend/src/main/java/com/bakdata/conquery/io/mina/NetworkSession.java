package com.bakdata.conquery.io.mina;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class NetworkSession implements MessageSender<NetworkMessage<?>> {
	public static final int MAX_MESSAGE_LENGTH = 30;
	public static final int MAX_QUEUE_LENGTH = 20;
	@Getter
	private final IoSession session;
	private final LinkedBlockingQueue<NetworkMessage<?>> queuedMessages = new LinkedBlockingQueue<>(MAX_QUEUE_LENGTH);

	@NotNull
	private static String shorten(String desc) {
		if (desc.length() <= MAX_MESSAGE_LENGTH) {
			return desc;
		}

		return desc.substring(0, MAX_MESSAGE_LENGTH) + "…";

	}

	@Override
	public WriteFuture send(final NetworkMessage<?> message) {
		try {
			while (!queuedMessages.offer(message, 2, TimeUnit.MINUTES)) {
				log.debug("Waiting for full writing queue for {} currently filled by:\n\t- {}",
						  message,
						  log.isTraceEnabled()
						  ? new ArrayList<>(queuedMessages).stream()
														   .map(Objects::toString)
														   .map(NetworkSession::shorten)
														   .collect(Collectors.joining("\n\t\t- "))
						  : "%s messages".formatted(queuedMessages.size())
				);
			}
		}
		catch (InterruptedException e) {
			log.error("Unexpected interruption, while trying to queue: {}", message, e);
			return DefaultWriteFuture.newNotWrittenFuture(session, e);
		}
		WriteFuture future = session.write(message);
    
		future.addListener(f -> {
			if(f instanceof WriteFuture writeFuture && ! writeFuture.isWritten()) {
				log.error("Could not write message: {} ({} -> {})", message, session.getLocalAddress(), session.getRemoteAddress(), writeFuture.getException());
			}
		});
		future.addListener(f -> queuedMessages.remove(message));

		return future;
	}

	@Override
	public void trySend(final NetworkMessage<?> message) {
		if (isConnected()) {
			session.write(message);
		}
	}

	@Override
	public boolean isConnected() {
		return session != null && session.isConnected();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return session.getRemoteAddress();
	}

	public SocketAddress getLocalAddress() {
		return session.getLocalAddress();
	}

	@Override
	public void awaitClose() {
		session.closeOnFlush().awaitUninterruptibly();
	}
}