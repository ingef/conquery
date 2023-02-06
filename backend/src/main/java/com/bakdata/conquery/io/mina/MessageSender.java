package com.bakdata.conquery.io.mina;

import java.net.SocketAddress;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.apache.mina.core.future.WriteFuture;

public interface MessageSender<MESSAGE> {

	WriteFuture send(MESSAGE message);
	void trySend(MESSAGE message);
	@JsonIgnore
	SocketAddress getRemoteAddress();
	void awaitClose();
	@JsonIgnore
	boolean isConnected();
	
	public static interface Transforming<MESSAGE, TARGET> extends MessageSender<MESSAGE> {
		@JsonIgnore
		MessageSender<TARGET> getMessageParent();
		TARGET transform(MESSAGE message);
		
		@Override
		default WriteFuture send(MESSAGE message) {
			return getMessageParent().send(transform(message));
		}
		
		@Override
		default void trySend(MESSAGE message) {
			getMessageParent().trySend(transform(message));
		}
		
		@Override
		default SocketAddress getRemoteAddress() {
			return getMessageParent().getRemoteAddress();
		}
		
		@Override
		default void awaitClose() {
			getMessageParent().awaitClose();
		}
		
		@Override
		default boolean isConnected() {
			return getMessageParent().isConnected();
		}
	}
	
	@AllArgsConstructor
	@ToString(of = "session")
	public static abstract class Simple<MESSAGE extends NetworkMessage<?>> implements MessageSender<MESSAGE> {
		
		@JsonIgnore @Setter @Getter @NonNull
		protected NetworkSession session;
		
		@Override
		public WriteFuture send(MESSAGE message) {
			return session.send(message);
		}
		
		@Override
		public void trySend(MESSAGE message) {
			session.trySend(message);
		}
		
		@Override
		public SocketAddress getRemoteAddress() {
			return session.getRemoteAddress();
		}
		
		@Override
		public void awaitClose() {
			session.awaitClose();
		}
		
		@Override
		public boolean isConnected() {
			return session.isConnected();
		}
	}
}
