package com.bakdata.conquery.util.extensions;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;

@RequiredArgsConstructor
public class MockServerExtension implements BeforeAllCallback, AfterAllCallback {

	@Delegate
	private final ClientAndServer server;
	private final Consumer<ClientAndServer> setup;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		setup.accept(server);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		server.stop();
	}

	public String baseUrl(){
		return "http://localhost:%d".formatted(server.getPort());
	}
}
