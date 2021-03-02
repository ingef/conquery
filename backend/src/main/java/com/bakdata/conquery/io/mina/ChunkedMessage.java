package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.Data;
import org.apache.mina.core.buffer.IoBuffer;

@Data
public class ChunkedMessage {

	private final ListeningExecutorService service;
	private final ObjectReader reader;
	private Thread readingThread;

	private final InputStream inputStream;
	private final PipedOutputStream outputStream;

	public ChunkedMessage(ListeningExecutorService service, ObjectReader reader) throws IOException {
		this.service = service;
		this.reader = reader;
		outputStream = new PipedOutputStream();
		inputStream = new PipedInputStream(outputStream);

		start();
	}

	private ListenableFuture<NetworkMessage<?>> result;

	public void start() {
		result = service.submit(() -> reader.readValue(inputStream));
	}

	public void addBuffer(IoBuffer buffer, int length) throws IOException {
		outputStream.write(buffer.array(), buffer.arrayOffset(), length);
	}
}