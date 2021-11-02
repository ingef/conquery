package com.bakdata.conquery.integration.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.github.powerlibraries.io.In;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceFile {
	private final String path;

	private final String root;

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static ResourceFile create(String path, @JacksonInject(value = "root", useInput = OptBoolean.FALSE) String root) {
		return new ResourceFile(path, root);
	}

	@JsonIgnore
	public String getPath() {
		if (path.startsWith("/")) {
			return path;
		}
		else {
			return "/" + root + path;
		}
	}

	public String getName() {
		return new File(getPath()).getName();
	}

	public InputStream stream() throws IOException {
		return In.resource(getPath()).asStream();
	}
}
