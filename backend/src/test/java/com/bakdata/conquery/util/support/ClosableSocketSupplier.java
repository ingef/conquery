package com.bakdata.conquery.util.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import lombok.SneakyThrows;

/**
 * Small helper to find open ports by opening random ports together in one context.
 * A previous implementation opened and closed ports individually which could cause a port binding collision
 * much easier.
 */
public class ClosableSocketSupplier implements Supplier<ServerSocket>, AutoCloseable {

	private final List<ServerSocket> openSockets = new ArrayList<>();

	@Override
	public void close() {
		openSockets.forEach((s) -> {
			try {
				s.close();
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});
		openSockets.clear();
	}

	@Override
	@SneakyThrows
	public ServerSocket get() {
		final ServerSocket serverSocket = new ServerSocket(0);
		openSockets.add(serverSocket);
		return serverSocket;
	}
}
