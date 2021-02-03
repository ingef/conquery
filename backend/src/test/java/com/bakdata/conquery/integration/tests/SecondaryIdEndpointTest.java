package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.api.description.FESecondaryId;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.ui.DatasetsUIResource;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecondaryIdEndpointTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {

		final SecondaryIdDescription description = new SecondaryIdDescription();
		description.setDescription("description-DESCRIPTION");
		description.setName("description-NAME");
		description.setLabel("description-LABEL");

		final SecondaryIdDescriptionId id = new SecondaryIdDescriptionId(conquery.getDataset().getId(), description.getName());

		final Response post = uploadDescription(conquery, description);


		log.info("{}", post);
		assertThat(post)
				.describedAs("Response = `%s`", post)
				.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());

		{
			final Set<FESecondaryId> secondaryIds = fetchSecondaryIdDescriptions(conquery);

			log.info("{}", secondaryIds);
			description.setDataset(conquery.getDataset());
			assertThat(secondaryIds)
					.extracting(FESecondaryId::getId)
					.containsExactly(description.getId().toString());
		}
		{
			final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("admin"), DatasetsUIResource.class, "getDataset")
										   .scheme("http").host("localhost").port(conquery.getAdminPort())
										   .buildFromMap(Map.of("dataset", conquery.getDataset().getName()));

			final Response actual = conquery.getClient().target(uri).request().get();
			assertThat(actual)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}

		final Response delete = deleteDescription(conquery, id);

		assertThat(delete)
				.describedAs("Response = `%s`", delete)
				.returns(Response.Status.Family.SUCCESSFUL,response -> response.getStatusInfo().getFamily());

		{
			final Set<FESecondaryId> secondaryIds = fetchSecondaryIdDescriptions(conquery);

			log.info("{}", secondaryIds);

			assertThat(secondaryIds)
					.isEmpty();
		}
	}

	private Set<FESecondaryId> fetchSecondaryIdDescriptions(StandaloneSupport conquery) throws java.io.IOException {
		final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("api"), DatasetResource.class, "getRoot")
									   .scheme("http")
									   .host("localhost")
									   .port(conquery.getLocalPort())
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName()
									   ));


		// We cannot effectively parse a full FERoot so we resort to only parsing the field.
		final ObjectNode objectNode = conquery.getClient()
											  .target(uri)
											  .request()
											  .get(ObjectNode.class);

		// The injection is necessary to deserialize the dataset.
		ObjectMapper mapper = conquery.getNamespace().getNamespaces().injectInto(Jackson.MAPPER);
		mapper = conquery.getDataset().injectInto(mapper);

		return objectNode.get("secondaryIds")
						 .traverse(mapper.getFactory().getCodec())
						 .readValueAs(new TypeReference<Set<FESecondaryId>>() {});
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


	private Response deleteDescription(StandaloneSupport conquery, SecondaryIdDescriptionId id) {
		final URI uri = HierarchyHelper.fromHierachicalPathResourceMethod(UriBuilder.fromPath("admin"), AdminDatasetResource.class, "deleteSecondaryId")
									   .host("localhost")
									   .scheme("http")
									   .port(conquery.getAdminPort())
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName(),
											   "secondaryId", id
									   ));


		return conquery.getClient()
					   .target(uri)
					   .request()
					   .delete();
	}

}
