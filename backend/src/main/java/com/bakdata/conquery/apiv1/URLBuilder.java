package com.bakdata.conquery.apiv1;

import static com.bakdata.conquery.resources.ResourceConstants.API;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

public class URLBuilder {

	private UriBuilder builder;
	private Map<String, String> queryParams = new HashMap<>();

	public URLBuilder(UriBuilder builder) {
		this.builder = builder;
	}

	public static URLBuilder fromRequest(HttpServletRequest request) {
		return new URLBuilder(UriBuilder.fromUri(RequestHelper.getRequestURL(request)).path(API));
	}

	public URLBuilder to(URLBuilderPath path) {
		builder.path(path.getClazz()).path(path.getClazz(), path.getMethod());
		return this;
	}

	public URLBuilder set(String key, String value) {
		builder.resolveTemplate(key, value);
		queryParams.put(key, value);
		return this;
	}

	@SneakyThrows(MalformedURLException.class)
	public URL get() {
		return builder.buildFromMap(queryParams).toURL();
	}

	@Override
	public String toString() {
		return builder.buildFromMap(queryParams).toASCIIString();
	}

	@Data
	@AllArgsConstructor
	public static class URLBuilderPath {

		private final Class<?> clazz;
		private final String method;
	}
}
