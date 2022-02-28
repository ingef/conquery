package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterAutocompleteTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static String[] RAW_LINES = {
			"id,label,option",
			"a,label-1,ov-1",
			"aab,label-2,ov-2",
			"aaa,label-3 and label-4,ov-4",
			"baaa,label-5,ov-5",
			"b,label-6,ov-6"
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

		conquery.getNamespace().getFilterSearch()
				.updateSearch(conquery.getNamespaceStorage(), conquery.getNamespace().getJobManager(), conquery.getConfig().getCsv());

		conquery.waitUntilWorkDone();

		Concept<?> concept = conquery.getNamespace().getStorage().getAllConcepts().iterator().next();
		Connector connector = concept.getConnectors().iterator().next();
		AbstractSelectFilter<?> filter = (AbstractSelectFilter<?>) connector.getFilters().iterator().next();

		// Copy search csv from resources to tmp folder.
		final Path tmpCSv = Files.createTempFile("conquery_search", "csv");

		Files.write(
				tmpCSv,
				String.join(csvConf.getLineSeparator(), RAW_LINES).getBytes(),
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE
		);

		filter.setTemplate(new FilterTemplate(tmpCSv.toString(), "id", "{{label}}", "Hello this is {{option}}"));


		filter.initializeSourceSearch(csvConf, conquery.getNamespaceStorage(), conquery.getNamespace().getFilterSearch());


		ConceptsProcessor processor = new ConceptsProcessor(conquery.getNamespace().getNamespaces());

		// from csv
		{
			ConceptsProcessor.AutoCompleteResult
					resolved =
					processor.autocompleteTextFilter(filter, Optional.of("a"), OptionalInt.empty(), OptionalInt.empty());

			//check the resolved values
			assertThat(resolved.getValues().stream().map(FEValue::getValue)).containsExactlyInAnyOrder("a", "aaa", "aab");
		}

		// from column values
		{
			ConceptsProcessor.AutoCompleteResult
					resolved =
					processor.autocompleteTextFilter(filter, Optional.of("f"), OptionalInt.empty(), OptionalInt.empty());

			//check the resolved values
			assertThat(resolved.getValues().stream().map(FEValue::getValue))
					.containsExactlyInAnyOrder("f", "fm");
		}
	}
}