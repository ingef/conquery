package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.api.description.FERoot;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecondaryIdEndpointTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {


		final SecondaryIdDescription description = new SecondaryIdDescription(null, "description-description");
		description.setName("description-name");
		description.setLabel("description-LABEL");

		final Response post = uploadDescription(conquery, description);


		log.info("{}", post);
		assertThat(post)
				.describedAs("Response = `%s`", post)
				.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());


		final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath(""), DatasetResource.class, "getRoot")
									   .scheme("http")
									   .host("localhost")
									   .port(conquery.getLocalPort())
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName()
									   ));

		assertThat(conquery.getClient()
				.target(uri)
				.request()
				.get(FERoot.class)
				.getSecondaryIds())
				.contains(description);


	}

	private Response uploadDescription(StandaloneSupport conquery, SecondaryIdDescription description) {
		final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("admin").host("localhost").scheme("http").port(conquery.getAdminPort()), AdminDatasetResource.class, "addSecondaryId")

									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName()
									   ));




		return conquery.getClient()
					   .target(uri)
					   .request(MediaType.APPLICATION_JSON_TYPE)
					   .post(Entity.entity(
							   description, MediaType.APPLICATION_JSON_TYPE
									  ));
	}

}
