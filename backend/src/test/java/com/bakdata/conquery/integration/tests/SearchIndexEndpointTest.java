package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.SEARCH_INDEX_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.admin.rest.AdminTablesResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchIndexEndpointTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {


		{
			// Start with nothing
			final List<SearchIndexId> searchIndexes = fetchSearchIndexes(conquery);

			log.info("{}", searchIndexes);
			assertThat(searchIndexes)
					.isEmpty();
		}



		{
			// Add indexes
			final SearchIndex searchIndex1 = new FilterTemplate(
					URI.create("/test"),
					"test",
					"test",
					"test"
			);
			searchIndex1.setName("index1");

			final SearchIndex searchIndex2 = new FilterTemplate(
					URI.create("/test2"),
					"test2",
					"test2",
					"test2"
			);
			searchIndex2.setName("index2");

			// Add index1
			final Response post1 = uploadSearchIndex(conquery, searchIndex1);
			assertThat(post1)
					.describedAs("Response = `%s`", post1)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());

			// Try to readd index1
			final Response repost1 = uploadSearchIndex(conquery, searchIndex1);
			assertThat(repost1)
					.describedAs("Response = `%s`", repost1)
					.returns(Response.Status.Family.CLIENT_ERROR, response -> response.getStatusInfo().getFamily());

			// Add index2
			final Response post2 = uploadSearchIndex(conquery, searchIndex2);
			assertThat(post2)
					.describedAs("Response = `%s`", post2)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}

		{
			// Check added index
			final List<SearchIndexId> searchIndexes = fetchSearchIndexes(conquery);

			log.info("{}", searchIndexes);
			assertThat(searchIndexes)
					.containsExactly(
							new SearchIndexId(conquery.getDataset(), "index1" ),
							new SearchIndexId(conquery.getDataset(), "index2" )
					);
		}

		{
			// Delete index1
			Response delete = deleteSearchIndex(conquery, new SearchIndexId(conquery.getDataset(), "index1"));
			assertThat(delete)
					.describedAs("Response = `%s`",delete)
					.returns(Response.Status.Family.SUCCESSFUL, response -> response.getStatusInfo().getFamily());
		}

		{
			// Check added index
			final List<SearchIndexId> searchIndexes = fetchSearchIndexes(conquery);

			log.info("{}", searchIndexes);
			assertThat(searchIndexes)
					.containsExactly(
							new SearchIndexId(conquery.getDataset(), "index2" )
					);
		}
	}

	private static Response uploadSearchIndex(StandaloneSupport conquery, SearchIndex index) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "addSearchIndex")
									   .buildFromMap(Map.of(
											   DATASET, conquery.getDataset().getName()
									   ));

		return conquery.getClient()
					   .target(uri)
					   .request(MediaType.APPLICATION_JSON_TYPE)
					   .post(Entity.entity(
							   index, MediaType.APPLICATION_JSON_TYPE
					   ));
	}

	private static List<SearchIndexId> fetchSearchIndexes(StandaloneSupport conquery) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "listSearchIndexes")
									   .buildFromMap(Map.of(
											   DATASET, conquery.getDataset().getName()
									   ));


		final List<SearchIndexId> indexes = conquery.getClient()
											  .target(uri)
											  .request()
											  .get(new GenericType<>(){});

		return indexes;
	}

	private static Response deleteSearchIndex(StandaloneSupport conquery, SearchIndexId id) {
		final URI uri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "deleteSearchIndex")
									   .buildFromMap(Map.of(
											   DATASET, conquery.getDataset().getName(),
											   SEARCH_INDEX_ID, id
									   ));


		return conquery.getClient()
					   .target(uri)
					   .request()
					   .delete();
	}

}
