package com.bakdata.conquery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.github.powerlibraries.io.In;

public class FrontendExistsTest {

	private static final String INDEX = "/frontend/app/static/index.html";

	@Test
	public void testIfFrontendExists() throws IOException {
		URL index = FrontendExistsTest.class.getResource(INDEX);
		assertThat(index).as("frontend file '%s' exists", INDEX).isNotNull();

		assertThat(In.resource(index).readAll()).as("check %s for correct content", INDEX).contains("conquery", "bakdata");
	}
}