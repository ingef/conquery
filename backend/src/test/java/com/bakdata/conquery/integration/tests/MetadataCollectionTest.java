package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetadataCollectionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.WORKER, StandaloneSupport.Mode.SQL);
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/matchingstats/icd.concept.json").withUTF8().readAll();

		DatasetId dataset = conquery.getDataset().getId();
		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		test.importRequiredData(conquery);

		//ensure the metadata is collected
		conquery.getDatasetsProcessor()
				.getStorageListener()
				.onUpdateMatchingStats(conquery.getDataset());
		conquery.waitUntilWorkDone();

		Collection<Concept<?>> allConcepts = conquery.getNamespace().getStorage().getAllConcepts();
		TreeConcept concept = (TreeConcept) allConcepts.iterator().next();

		//check the number of matched events from root node to the deepest child node
		assertThat(concept.getMatchingStats().countEvents()).isEqualTo(10);
		assertThat(concept.getMatchingStats().countEntities()).isEqualTo(3);
		// concepts 1. child (F00-F99)
		ConceptTreeChild f00_99 = concept.getChildren().get(0);
		assertThat(f00_99.getMatchingStats().countEvents()).isEqualTo(8);
		assertThat(f00_99.getMatchingStats().countEntities()).isEqualTo(3);
		// 1. child's child (F20-29)
		ConceptTreeChild f20_29 = f00_99.getChildren().get(0);
		assertThat(f20_29.getMatchingStats().countEvents()).isEqualTo(7);
		assertThat(f20_29.getMatchingStats().countEntities()).isEqualTo(2);
		// 1. child's child's child (yeah it's getting wild)
		ConceptTreeChild f20 = f20_29.getChildren().get(0);
		assertThat(f20.getMatchingStats().countEvents()).isEqualTo(5);
		assertThat(f20.getMatchingStats().countEntities()).isEqualTo(1);
		// 1. child's child's child's children (I promise it won't get worse)
		assertThat(f20.getChildren()).allSatisfy(child -> {
			assertThat(child.getMatchingStats().countEvents()).isEqualTo(1);
			assertThat(child.getMatchingStats().countEntities()).isEqualTo(1);
		});

		//check the date ranges
		assertThat(concept.getMatchingStats().spanEvents())
				.isEqualTo(CDateRange.of(LocalDate.parse("2009-05-18"), LocalDate.parse("2023-08-20")));
		assertThat(f20.getMatchingStats().spanEvents())
				.isEqualTo(CDateRange.of(LocalDate.parse("2010-07-01"), LocalDate.parse("2023-02-18")));
	}
}
