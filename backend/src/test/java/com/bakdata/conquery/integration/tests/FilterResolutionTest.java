package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.FilterResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterResolutionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private final String[] lines = new String[]{
			"HEADER",
			"a",
			"aab",
			"aaa",
			"b"
	};

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		final String
				testJson =
				In.resource("/tests/query/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY.test.json")
				  .withUTF8()
				  .readAll();

		final DatasetId dataset = conquery.getDataset().getId();

		final ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);

		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		final CSVConfig csvConf = conquery.getConfig().getCsv();

		test.importRequiredData(conquery);

		conquery.waitUntilWorkDone();


		final Concept<?> concept = conquery.getNamespace().getStorage().getAllConcepts().iterator().next();
		final Connector connector = concept.getConnectors().iterator().next();
		final SelectFilter<?> filter = (SelectFilter<?>) connector.getFilters().iterator().next();

		// Copy search csv from resources to tmp folder.
		final Path tmpCSv = Files.createTempFile("conquery_search", "csv");
		Files.write(tmpCSv, String.join(csvConf.getLineSeparator(), lines)
								  .getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);


		final IndexService indexService = new IndexService(conquery.getConfig().getCsv().createCsvParserSettings(), "emptyDefaultLabel");

		filter.setTemplate(new FilterTemplate(conquery.getDataset(), "test", tmpCSv.toUri(), "HEADER", "", "",  2, true, indexService));

		final URI matchingStatsUri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder()
															, AdminDatasetResource.class, "updateMatchingStats")
													.buildFromMap(Map.of(DATASET, conquery.getDataset().getId()));

		final Response post = conquery.getClient().target(matchingStatsUri)
									  .request(MediaType.APPLICATION_JSON_TYPE)
									  .post(null);
		post.close();

		conquery.waitUntilWorkDone();

		final URI resolveUri =
				HierarchyHelper.hierarchicalPath(
									   conquery.defaultApiURIBuilder(),
									   FilterResource.class, "resolveFilterValues"
							   )
							   .buildFromMap(
									   Map.of(
											   DATASET, conquery.getDataset().getId(),
											   CONCEPT, concept.getId(),
											   TABLE, filter.getConnector().getTable().getId(),
											   FILTER, filter.getId()
									   )
							   );

		// from csv
		{
			final Response fromCsvResponse = conquery.getClient().target(resolveUri)
													 .request(MediaType.APPLICATION_JSON_TYPE)
													 .post(Entity.entity(new FilterResource.FilterValues(List.of("a", "aaa", "unknown")), MediaType.APPLICATION_JSON_TYPE));

			final ConceptsProcessor.ResolvedFilterValues resolved = fromCsvResponse.readEntity(ConceptsProcessor.ResolvedFilterValues.class);

			//check the resolved values
			// "aaa" is hit by "a" and "aaa" therefore should be first
			assertThat(resolved.resolvedFilter().value().stream().map(FrontendValue::getValue)).containsExactly("aaa", "a");
			assertThat(resolved.unknownCodes()).containsExactly("unknown");
		}

		// from column values
		{
			final Response fromCsvResponse = conquery.getClient().target(resolveUri)
													 .request(MediaType.APPLICATION_JSON_TYPE)
													 .post(Entity.entity(new FilterResource.FilterValues(List.of("f", "unknown")), MediaType.APPLICATION_JSON_TYPE));

			final ConceptsProcessor.ResolvedFilterValues resolved = fromCsvResponse.readEntity(ConceptsProcessor.ResolvedFilterValues.class);

			//check the resolved values
			assertThat(resolved.resolvedFilter().value().stream().map(FrontendValue::getValue)).contains("f");
			assertThat(resolved.unknownCodes()).containsExactly("unknown");
		}
	}
}