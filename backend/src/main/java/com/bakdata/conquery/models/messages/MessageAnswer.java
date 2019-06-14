package com.bakdata.conquery.models.messages;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.mina.core.future.WriteFuture;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@ToString
@Slf4j
public class MessageAnswer {
	
	public static enum Status {OK, FAILED}
	
	private final UUID messageId;
	private final WriteFuture writeFuture;
	private final CompletableFuture<Status> status = new CompletableFuture<>();
	
	/**
	 * wait until the receiver of the message answers with a positive
	 * or negative response
	 */
	public void awaitAnyResult() {
		try {
			Uninterruptibles.getUninterruptibly(status);
		}
		catch (ExecutionException e) {
			//we can explicitely ignore this exception
			log.debug("Ignored message exception", e);
		}
	}

	public void awaitSuccess() {
		try {
			if(Uninterruptibles.getUninterruptibly(status) != Status.OK) {
				throw new RuntimeException(
					"Message answer for "+messageId+" was "+Futures.getDone(status)
				);
			}
		}
		catch (ExecutionException e) {
			throw new RuntimeException(
				"Exception while waiting for message answer "+messageId,
				e
			);
		}
	}
}
