package com.bakdata.conquery.resources.admin.rest;

import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.api.openapi.ScriptApi;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminScriptResource implements ScriptApi {

	private final AdminProcessor processor;

	
	@HeaderParam(HttpHeaders.ACCEPT)
	private String acceptHeader;

	@Override
	public Object executeScript(String script) {
		return switch (acceptHeader) {
			case MediaType.TEXT_PLAIN -> Objects.toString(processor.executeScript(script));
			case MediaType.APPLICATION_JSON -> processor.executeScript(script);
			default -> throw new IllegalArgumentException("Unexpected value: " + acceptHeader);
		};
	}
}
