package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.concepts.virtual.VirtualConceptConnector;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterResolutionTest implements ProgrammaticIntegrationTest, IntegrationTest.Simple {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY/MULTI_SELECT_DATE_RESTRICTION_OR_CONCEPT_QUERY.test.json").withUTF8().readAll();
		
		DatasetId dataset = conquery.getDataset().getId();
		
		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		
		test.importRequiredData(conquery);
		FilterSearch
			.init(Collections.singleton(conquery.getNamespace().getDataset()))
			.awaitTermination(1, TimeUnit.MINUTES);

		
		VirtualConcept concept = (VirtualConcept) conquery.getNamespace().getStorage().getAllConcepts().iterator().next();
		VirtualConceptConnector connector = concept.getConnectors().iterator().next();
		AbstractSelectFilter<?> filter = (AbstractSelectFilter<?>) connector.getFilter();
		ConceptsProcessor processor = new ConceptsProcessor(conquery.getNamespace().getNamespaces());
		
		ResolvedConceptsResult resolved = processor.resolveFilterValues(filter, List.of("m", "mf", "unknown"));
		
		//check the resolved values
		assertThat(resolved).isNotNull();
		assertThat(resolved.getResolvedFilter().getValue().stream().map(FEValue::getValue)).containsExactlyInAnyOrder("m", "mf");
		assertThat(resolved.getUnknownCodes()).containsExactlyInAnyOrder("unknown");
		
	}
}