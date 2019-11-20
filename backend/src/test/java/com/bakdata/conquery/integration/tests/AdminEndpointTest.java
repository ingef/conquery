package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;

public class AdminEndpointTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		String expectedEndpoints = new String(In.resource("/tests/endpoints/adminEndpointInfo.txt").asStream().readAllBytes());
		String actualEndpoints = testConquery.getStandaloneCommand().getMaster().getAdmin().getJerseyConfig().getEndpointsInfo();
		assertThat(actualEndpoints).isEqualTo(expectedEndpoints);
	}

}
