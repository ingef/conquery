package com.bakdata.conquery.integration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.powerlibraries.io.In;

import lombok.Getter;

public class ResourceFile {
	@Getter(onMethod_=@JsonValue)
	private final String path;
	
	@JsonCreator
	public ResourceFile(String path) {
		this.path = StringUtils.prependIfMissing(path, "/");
		assertThat(ResourceFile.class.getResource(this.path))
			.as("Resource "+this.path+" does not exist")
			.isNotNull();
	}

	public String getName() {
		return new File(path).getName();
	}

	public InputStream stream() throws IOException {
		return In.resource(path).asStream();
	}
}
