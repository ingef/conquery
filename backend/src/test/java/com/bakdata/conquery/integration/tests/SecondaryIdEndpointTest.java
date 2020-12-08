package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecondaryIdEndpointTest extends IntegrationTest.Simple {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {


		final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod("", AdminDatasetResource.class, "addSecondaryId")
									   .scheme("http")
									   .host("localhost")
									   .port(conquery.getAdminPort())
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName()
									   ));


		final SecondaryIdDescription description = new SecondaryIdDescription(null, "description");

		description.setName("description-name");
		description.setLabel("description-LABEL");

		final Response post = conquery.getClient()
									  .target(uri)
									  .request(MediaType.APPLICATION_JSON_TYPE)
									  .post(Entity.entity(
											  description, MediaType.APPLICATION_JSON_TYPE
									  ));


		log.info("{}", post);
		assertThat(post)
				.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());

	}

}
