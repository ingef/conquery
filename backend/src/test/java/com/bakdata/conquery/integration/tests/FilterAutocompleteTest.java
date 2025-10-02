package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterAutocompleteTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static final String[] RAW_LINES = {
			"id,label,option",
			"a,lbl-1,ov-1",
			"aab,lbl-2,ov-2",
			"aaa,lbl-3 & lbl-4,ov-4",
			"baaa,lbl-5,ov-5",
			"b,lbl-6,ov-6",
			"female,female-label,female-option",
			"male,male-label,male-option"
	};

	@Override
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.WORKER, StandaloneSupport.Mode.SQL);
	}

	@Override
	public ConqueryConfig overrideConfig(ConqueryConfig conf, File workdir) {
		conf.getIndex().setEmptyLabel("emptyDefaultLabel");
		return conf;
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		final SelectFilter<?> filter = setupSearch(conquery);

		final Concept<?> concept = filter.getConnector().getConcept();

		final URI autocompleteUri =
				HierarchyHelper.hierarchicalPath(
									   conquery.defaultApiURIBuilder(),
									   FilterResource.class, "autocompleteTextFilter"
							   )
							   .buildFromMap(
									   Map.of(
											   DATASET, conquery.getDataset(),
											   CONCEPT, concept.getId(),
											   TABLE, filter.getConnector().getResolvedTable().getId(),
											   FILTER, filter.getId()
									   )
							   );

		final Invocation.Builder autocompleteRequestBuilder = conquery.getClient().target(autocompleteUri)
																	  .request(MediaType.APPLICATION_JSON_TYPE);
		// Data starting with a is in reference csv
		{
			try (final Response fromCsvResponse = autocompleteRequestBuilder.post(Entity.entity(new FilterResource.AutocompleteRequest(
																										Optional.of("a"),
																										OptionalInt.empty(),
																										OptionalInt.empty()
																								), MediaType.APPLICATION_JSON_TYPE
			))) {

				final ConceptsProcessor.AutoCompleteResult resolvedFromCsv = fromCsvResponse.readEntity(ConceptsProcessor.AutoCompleteResult.class);

				// "aaa" occurs after "aab" due to it consisting only of duplicate entries.
				// The empty string results from `No V*a*lue` and `..Def*au*lt..`

				assertThat(resolvedFromCsv.values().stream().map(FrontendValue::getValue))
						.containsExactly("a", "aab", "aaa", "male", "female", "baaa");

			}
		}


		// Data starting with f  is in column values
		{
			try (final Response fromCsvResponse = autocompleteRequestBuilder
					.post(Entity.entity(new FilterResource.AutocompleteRequest(
												Optional.of("f"),
												OptionalInt.empty(),
												OptionalInt.empty()
										), MediaType.APPLICATION_JSON_TYPE
					))) {

				final ConceptsProcessor.AutoCompleteResult resolvedFromValues = fromCsvResponse.readEntity(ConceptsProcessor.AutoCompleteResult.class);

				//check the resolved values
				assertThat(resolvedFromValues.values().stream().map(FrontendValue::getValue))
						.containsExactly("f", "female", "fm");
			}
		}


		// Data starting with a is in reference csv
		{
			try (final Response fromCsvResponse = autocompleteRequestBuilder
					.post(Entity.entity(new FilterResource.AutocompleteRequest(
												Optional.of(""),
												OptionalInt.empty(),
												OptionalInt.empty()
										), MediaType.APPLICATION_JSON_TYPE
					))) {

				final ConceptsProcessor.AutoCompleteResult resolvedFromCsv = fromCsvResponse.readEntity(ConceptsProcessor.AutoCompleteResult.class);
				// This is probably the insertion order
				assertThat(resolvedFromCsv.values().stream().map(FrontendValue::getValue))
						.containsExactlyInAnyOrder("","aaa", "a", "aab", "b", "baaa", "female", "male", "f", "fm", "m", "mf");
			}
		}
	}

	private static SelectFilter<?> setupSearch(StandaloneSupport conquery) throws Exception {
		//read test specification
		final String testJson =
				LoadingUtil.readResource("/tests/query/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY.test.json");
		final DatasetId dataset = conquery.getDataset();

		final ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);

		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);

		conquery.waitUntilWorkDone();

		final CSVConfig csvConf = conquery.getConfig().getCsv();

		NamespaceStorage namespaceStorage = conquery.getNamespace().getStorage();
		Stream<Concept<?>> allConcepts = namespaceStorage.getAllConcepts();
		final Concept<?> concept = allConcepts.filter(c -> c.getName().equals("geschlecht_select")).findFirst().orElseThrow();
		allConcepts.close();
		final Connector connector = concept.getConnectors().getFirst();
		final SelectFilter<?> filter = (SelectFilter<?>) connector.getFilters().iterator().next();

		// Copy search csv from resources to tmp folder.
		// TODO this file is not deleted at the end of this test
		final Path tmpCsv = Files.createTempFile("conquery_search", "csv");

		Files.write(
				tmpCsv,
				String.join(csvConf.getLineSeparator(), RAW_LINES).getBytes(),
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE
		);

		IndexService indexService = conquery.getDatasetRegistry().getIndexService();

		final FilterTemplate
				filterTemplate =
				new FilterTemplate(tmpCsv.toUri(), "id", "{{label}}", "Hello this is {{option}}", 2, true, indexService, conquery.getConfig());

		filterTemplate.setDataset(conquery.getDataset());
		filterTemplate.setName("test");
		filter.setTemplate(filterTemplate.getId());

		// We need to persist the modification before we submit the update matching stats request
		namespaceStorage.addSearchIndex(filterTemplate);
		namespaceStorage.updateConcept(concept);

		final URI matchingStatsUri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder()
															, AdminDatasetResource.class, "postprocessNamespace"
													)
													.buildFromMap(Map.of(DATASET, conquery.getDataset()));

		conquery.getClient().target(matchingStatsUri)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(null)
				.close();

		conquery.waitUntilWorkDone();

		return filter;
	}
}
