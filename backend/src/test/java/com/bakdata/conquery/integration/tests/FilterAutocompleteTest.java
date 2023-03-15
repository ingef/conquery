package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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
public class FilterAutocompleteTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static final String[] RAW_LINES = {
			"id,label,option",
			"a,lbl-1,ov-1",
			"aab,lbl-2,ov-2",
			"aaa,lbl-3 & lbl-4,ov-4",
			"baaa,lbl-5,ov-5",
			"b,lbl-6,ov-6"
	};

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test specification
		String
				testJson =
				In.resource("/tests/query/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY.test.json")
				  .withUTF8()
				  .readAll();

		DatasetId dataset = conquery.getDataset().getId();

		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);

		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);
		CSVConfig csvConf = conquery.getConfig().getCsv();

		conquery.waitUntilWorkDone();

		Concept<?> concept = conquery.getNamespace().getStorage().getAllConcepts().iterator().next();
		Connector connector = concept.getConnectors().iterator().next();
		SelectFilter<?> filter = (SelectFilter<?>) connector.getFilters().iterator().next();

		// Copy search csv from resources to tmp folder.
		final Path tmpCSv = Files.createTempFile("conquery_search", "csv");

		Files.write(
				tmpCSv,
				String.join(csvConf.getLineSeparator(), RAW_LINES).getBytes(),
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE
		);

		final IndexService indexService = new IndexService(conquery.getConfig().getCsv().createCsvParserSettings());

		filter.setTemplate(new FilterTemplate(conquery.getDataset(), "test", tmpCSv.toUri(), "id", "{{label}}", "Hello this is {{option}}", 2, true, indexService));

		final URI matchingStatsUri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder()
															, AdminDatasetResource.class, "updateMatchingStats")
													.buildFromMap(Map.of(DATASET, conquery.getDataset().getId()));

		conquery.getClient().target(matchingStatsUri)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(null)
				.close();

		conquery.waitUntilWorkDone();

		final URI autocompleteUri =
				HierarchyHelper.hierarchicalPath(
									   conquery.defaultApiURIBuilder(),
									   FilterResource.class, "autocompleteTextFilter"
							   )
							   .buildFromMap(
									   Map.of(
											   DATASET, conquery.getDataset().getId(),
											   CONCEPT, concept.getId(),
											   TABLE, filter.getConnector().getTable().getId(),
											   FILTER, filter.getId()
									   )
							   );

		// Data starting with a is in reference csv
		{
			final Response fromCsvResponse = conquery.getClient().target(autocompleteUri)
													 .request(MediaType.APPLICATION_JSON_TYPE)
													 .post(Entity.entity(new FilterResource.AutocompleteRequest(
															 Optional.of("a"),
															 OptionalInt.empty(),
															 OptionalInt.empty()
													 ), MediaType.APPLICATION_JSON_TYPE));

			final ConceptsProcessor.AutoCompleteResult resolvedFromCsv = fromCsvResponse.readEntity(ConceptsProcessor.AutoCompleteResult.class);
			assertThat(resolvedFromCsv.getValues().stream().map(FrontendValue::getValue)).containsExactly("a", "aaa", "aab", "baaa");
		}


		// Data starting with f  is in column values
		{
			final Response fromCsvResponse = conquery.getClient().target(autocompleteUri)
													 .request(MediaType.APPLICATION_JSON_TYPE)
													 .post(Entity.entity(new FilterResource.AutocompleteRequest(
															 Optional.of("f"),
															 OptionalInt.empty(),
															 OptionalInt.empty()
													 ), MediaType.APPLICATION_JSON_TYPE));

			final ConceptsProcessor.AutoCompleteResult resolvedFromValues = fromCsvResponse.readEntity(ConceptsProcessor.AutoCompleteResult.class);

			//check the resolved values
			assertThat(resolvedFromValues.getValues().stream().map(FrontendValue::getValue))
					.containsExactly("f", "fm");
		}
	}
}