package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.stream.Stream;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetadataCollectionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		DatasetId dataset = conquery.getDataset();

		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);

		//ensure the metadata is collected
		DistributedNamespace namespace = (DistributedNamespace) conquery.getNamespace();
		Stream<Concept<?>> allConcepts = conquery.getNamespace().getStorage().getAllConcepts();
		namespace.getWorkerHandler()
				 .sendToAll(new UpdateMatchingStatsMessage(allConcepts.map(Concept::getId).toList()));
		allConcepts.close();

		conquery.waitUntilWorkDone();

		allConcepts = conquery.getNamespace().getStorage().getAllConcepts();
		TreeConcept concept = (TreeConcept) allConcepts.findFirst().orElseThrow();
		allConcepts.close();

		//check the number of matched events
		assertThat(concept.getMatchingStats().countEvents()).isEqualTo(4);
		assertThat(concept.getChildren()).allSatisfy(c -> assertThat(c.getMatchingStats().countEvents()).isEqualTo(2));
		
		//check the date ranges
		assertThat(concept.getMatchingStats().spanEvents())
			.isEqualTo(CDateRange.of(LocalDate.parse("2010-07-15"), LocalDate.parse("2013-11-10")));
		assertThat(concept.getChildren().get(0).getMatchingStats().spanEvents())
			.isEqualTo(CDateRange.of(LocalDate.parse("2012-01-01"), LocalDate.parse("2013-11-10")));
		assertThat(concept.getChildren().get(1).getMatchingStats().spanEvents())
			.isEqualTo(CDateRange.of(LocalDate.parse("2010-07-15"), LocalDate.parse("2012-11-11")));
	}
}
