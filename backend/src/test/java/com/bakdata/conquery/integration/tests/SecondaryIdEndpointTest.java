package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.frontend.FrontendSecondaryId;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
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
		description.setDataset(conquery.getDataset());
		description.setName("name");
		description.setDescription("description");
		description.setLabel("label");

		{
			final Response post = uploadDescription(conquery, description);
			assertThat(post)
					.describedAs("Response = `%s`", post)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}
		final SecondaryIdDescription descriptionHidden = new SecondaryIdDescription();
		descriptionHidden.setDataset(conquery.getDataset());
		descriptionHidden.setName("hidden");
		descriptionHidden.setDescription("hidden");
		descriptionHidden.setLabel("label");
		descriptionHidden.setHidden(true);

		{
			final Response post = uploadDescription(conquery, descriptionHidden);
			assertThat(post)
					.describedAs("Response = `%s`", post)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}


		{
			// showHidden=false
			final Set<FrontendSecondaryId> secondaryIds = fetchSecondaryIdDescriptions(conquery, false);

			log.info("{}", secondaryIds);
			description.setDataset(conquery.getDataset());
			assertThat(secondaryIds)
					.extracting(FrontendSecondaryId::getId)
					.containsExactly(description.getId().toString());
		}

		{
			// showHidden=true
			final Set<FrontendSecondaryId> secondaryIds = fetchSecondaryIdDescriptions(conquery, true);

			log.info("{}", secondaryIds);
			assertThat(secondaryIds)
					.extracting(FrontendSecondaryId::getId)
					.containsExactly(description.getId().toString(), descriptionHidden.getId().toString());
		}

		// Upload Table referencing SecondaryId
		{
			// Build data manually so content is minimal (ie no dataset prefixes etc)
			ObjectNode tableNode = Jackson.MAPPER.createObjectNode();
			tableNode.put("name", "table");

			ObjectNode columnNode = Jackson.MAPPER.createObjectNode();

			columnNode.put("name", "column");
			columnNode.put("type", MajorTypeId.INTEGER.name());
			columnNode.put("secondaryId", description.getId().toString());

			tableNode.set("columns", columnNode);

			final Response response = uploadTable(conquery, tableNode);
			assertThat(response.getStatusInfo().getFamily())
					.describedAs(() -> response.readEntity(String.class))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);
		}
		{
			final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), DatasetsUIResource.class, "getDataset")
										   .buildFromMap(Map.of("dataset", conquery.getDataset().getName()));

			final Response actual = conquery.getClient().target(uri).request().get();
			assertThat(actual)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}

		{
			//First one fails because table depends on it
			assertThat(deleteDescription(conquery, description.getId()))
					.returns(Response.Status.Family.CLIENT_ERROR, response -> response.getStatusInfo().getFamily());

			deleteTable(conquery, new TableId(conquery.getDataset(), "table"));

			// We've deleted the table, now it should be successful
			assertThat(deleteDescription(conquery, description.getId()))
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());

			final Set<FrontendSecondaryId> secondaryIds = fetchSecondaryIdDescriptions(conquery, false);

			log.info("{}", secondaryIds);

			assertThat(secondaryIds)
					.isEmpty();
		}

	}

	private static Response uploadDescription(StandaloneSupport conquery, SecondaryIdDescription description) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "addSecondaryId")

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

	private static Set<FrontendSecondaryId> fetchSecondaryIdDescriptions(StandaloneSupport conquery, boolean showHidden) throws java.io.IOException {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetResource.class, "getRoot")
									   .queryParam("showHidden", showHidden)
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName()
									   ));


		// We cannot effectively parse a full FERoot so we resort to only parsing the field.
		final ObjectNode objectNode = conquery.getClient()
											  .target(uri)
											  .request()
											  .get(ObjectNode.class);

		// The injection is necessary to deserialize the dataset.
		ObjectMapper mapper = conquery.getDatasetRegistry().injectIntoNew(Jackson.MAPPER);
		mapper = conquery.getNamespace().getDataset().injectIntoNew(mapper);

		return objectNode.get("secondaryIds")
						 .traverse(mapper.getFactory().getCodec())
						 .readValueAs(new TypeReference<Set<FrontendSecondaryId>>() {
						 });
	}

	private static Response uploadTable(StandaloneSupport conquery, ObjectNode table) {
		final URI addTable = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "addTable")
											.buildFromMap(Map.of(ResourceConstants.DATASET, conquery.getDataset().getName()));

		return conquery.getClient()
					   .target(addTable)
					   .request(MediaType.APPLICATION_JSON)
					   .post(Entity.entity(table, MediaType.APPLICATION_JSON_TYPE));

	}

	private static Response deleteDescription(StandaloneSupport conquery, SecondaryIdDescriptionId id) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "deleteSecondaryId")
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName(),
											   "secondaryId", id
									   ));


		return conquery.getClient()
					   .target(uri)
					   .request()
					   .delete();
	}

	private static Response deleteTable(StandaloneSupport conquery, TableId id) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminTablesResource.class, "remove")
									   .buildFromMap(Map.of(
											   "dataset", conquery.getDataset().getName(),
											   "table", id
									   ));


		return conquery.getClient()
					   .target(uri)
					   .request()
					   .delete();
	}

}
