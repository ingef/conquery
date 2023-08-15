package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.integration.tests.EndpointTestHelper.EndPoint;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.DropwizardResourceConfig;

import java.util.List;

import static com.bakdata.conquery.integration.tests.EndpointTestHelper.READER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test assures, that we do not lose endpoints by accident, while there are
 * no tests against the admin api.
 */
public class AdminUIEndpointTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		List<EndPoint> expectedEndpoints = READER.readValue(In.resource("/tests/endpoints/adminUIEndpointInfo.json").asStream());

		DropwizardResourceConfig jerseyConfig = testConquery.getStandaloneCommand().getManagerNode().getAdmin().getJerseyConfigUI();

		List<EndPoint> resources = EndpointTestHelper.collectEndpoints(jerseyConfig);

		assertThat(resources).containsExactlyInAnyOrderElementsOf(expectedEndpoints);

	}

}
