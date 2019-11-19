package com.bakdata.conquery.io.mina;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.bakdata.conquery.models.messages.network.NetworkMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class NetworkSession implements MessageSender<NetworkMessage<?>> {
	@Getter
	private final IoSession session;
	private final LinkedBlockingQueue<NetworkMessage<?>> queuedMessages = new LinkedBlockingQueue<>(20);

	@Override
	public WriteFuture send(final NetworkMessage<?> message) {
		try {
			while(!queuedMessages.offer(message, 2, TimeUnit.MINUTES)) {
				log.debug("Waiting for full writing queue for {}\n\tcurrently filled by: {}",
						message,
						new ArrayList<>(queuedMessages)
							.stream()
							.map(Objects::toString)
							.collect(Collectors.joining("\n\t\t"))
				);
			}
		} catch (InterruptedException e) {
			log.error("Unexpected interruption", e);
			return send(message);
		}
		WriteFuture future = session
				.write(message);
		return future
			.addListener(f->queuedMessages.remove(message));
	}
	
	@Override
	public void trySend(final NetworkMessage<?> message) {
		if(isConnected()) {
			session.write(message);
		}
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

	@Override
	public boolean isConnected() {
		return session != null && session.isConnected();
	}
}