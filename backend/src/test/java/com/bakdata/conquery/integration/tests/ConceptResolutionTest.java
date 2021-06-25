package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConceptResolutionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();
		
		DatasetId dataset = conquery.getDataset().getId();
		
		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		
		test.importRequiredData(conquery);
		FilterSearch
			.updateSearch(conquery.getNamespace().getNamespaces(), Collections.singleton(conquery.getNamespace().getDataset()), conquery.getDatasetsProcessor().getJobManager(), conquery.getConfig().getCsv().createParser());

		conquery.waitUntilWorkDone();

		ConceptsProcessor processor = new ConceptsProcessor(conquery.getNamespace().getNamespaces());
		TreeConcept concept = (TreeConcept) conquery.getNamespace().getStorage().getAllConcepts().iterator().next();
		
		ResolvedConceptsResult resolved = processor.resolveConceptElements(concept, List.of("A1", "unknown"));
		
		//check the resolved values
		assertThat(resolved).isNotNull();
		assertThat(resolved.getResolvedConcepts().stream().map(IId::toString)).containsExactlyInAnyOrder("ConceptResolutionTest.test_tree.test_child1");
		assertThat(resolved.getUnknownCodes()).containsExactlyInAnyOrder("unknown");
		
	}
}