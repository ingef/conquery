package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

public class ApiEndpointTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		String expectedEndpoints = new String(In.resource("/tests/endpoints/apiEndpointInfo.txt").asStream().readAllBytes());
		String actualEndpoints = testConquery.getDropwizard().getEnvironment().jersey().getResourceConfig().getEndpointsInfo();
		assertThat(actualEndpoints).isEqualTo(expectedEndpoints);
	}

}
