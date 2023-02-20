package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;
import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.description.LazyTextDescription;


@Slf4j
public class EntityResolveTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/query/ENTITY_EXPORT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();

		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importInternToExternMappers(conquery, test.getInternToExternMappings());
			conquery.waitUntilWorkDone();

			final RequiredData content = test.getContent();
			importSecondaryIds(conquery, content.getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, content.getTables(), content.isAutoConcept());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, content.getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.updateMatchingStats(conquery);
			conquery.waitUntilWorkDone();

		}

		final URI entityExport = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetQueryResource.class, "resolveEntities")
												.buildFromMap(Map.of(ResourceConstants.DATASET, conquery.getDataset().getName()));

		// Api uses NsIdRef, so we have to use the real objects here.
		final Filter<?> filter = conquery.getDatasetRegistry().resolve(
				FilterId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree1.connector.values-filter")
		);


		final List<Map<String, String>> result;
		try (Response allEntityDataResponse = conquery.getClient().target(entityExport)
													  .request(MediaType.APPLICATION_JSON_TYPE)
													  .header("Accept-Language", "en-Us")
													  .post(Entity.json(
															  new FilterValue[]{
																	  // Bit lazy, but this explicitly or's two filters
																	  new FilterValue.CQMultiSelectFilter((Filter<String[]>) filter, new String[]{"A1"}),
																	  new FilterValue.CQMultiSelectFilter((Filter<String[]>) filter, new String[]{"B2"})
															  }
													  ))) {

			assertThat(allEntityDataResponse.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> allEntityDataResponse.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);

			result = allEntityDataResponse.readEntity(List.class);
		}


		assertThat(result).containsExactly(Map.of("ID", "1"), Map.of("ID", "4"));
	}

}
