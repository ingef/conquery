package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;

public class DefaultQueryTagsTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static final List<String> DEFAULT_TAGS = List.of("default-tag", "another-default-tag");

	@Override
	public ConqueryConfig overrideConfig(ConqueryConfig conf, File workdir) {
		return conf.withFrontend(conf.getFrontend().withDefaultTags(DEFAULT_TAGS));
	}

	@Override
	public void execute(StandaloneSupport conquery) {
		final URI defaultTagsUri = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetQueryResource.class, "getDefaultTags")
										  .buildFromMap(Map.of(DATASET, conquery.getDataset()));

		try (final Response response = conquery.getClient()
										  .target(defaultTagsUri)
										  .request(MediaType.APPLICATION_JSON_TYPE)
										  .get()) {
			assertThat(response.getStatusInfo().getFamily()).isEqualTo(Response.Status.Family.SUCCESSFUL);
			assertThat(response.readEntity(new GenericType<List<String>>() {
			})).containsExactlyElementsOf(DEFAULT_TAGS);
		}
	}
}
