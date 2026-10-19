package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingStatsIntegrationTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.SQL, StandaloneSupport.Mode.WORKER);
	}

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		String testJson = LoadingUtil.readResource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json");

		DatasetId dataset = conquery.getDataset();

		ConqueryTestSpec test = ConqueryTestSpec.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);
		LoadingUtil.updateMatchingStats(conquery);
		conquery.waitUntilWorkDone();

		TreeConcept concept;
		try (Stream<Concept<?>> allConcepts = conquery.getNamespace().getStorage().getAllConcepts()) {
			concept = (TreeConcept) allConcepts.findFirst().orElseThrow();
		}

		assertThat(concept.getMatchingStats()).isNotNull();
		assertThat(concept.getMatchingStats().countEvents()).isEqualTo(4);
		assertThat(concept.getMatchingStats().countEntities()).isEqualTo(4);
		assertThat(concept.getChildren()).allSatisfy(child -> {
			assertThat(child.getMatchingStats()).isNotNull();
			assertThat(child.getMatchingStats().countEvents()).isEqualTo(2);
			assertThat(child.getMatchingStats().countEntities()).isEqualTo(2);
		});

		assertThat(concept.getMatchingStats().spanEvents())
				.isEqualTo(CDateRange.of(LocalDate.parse("2010-07-15"), LocalDate.parse("2013-11-10")));
		assertThat(concept.getChildren().get(0).getMatchingStats().spanEvents())
				.isEqualTo(CDateRange.of(LocalDate.parse("2012-01-01"), LocalDate.parse("2013-11-10")));
		assertThat(concept.getChildren().get(1).getMatchingStats().spanEvents())
				.isEqualTo(CDateRange.of(LocalDate.parse("2010-07-15"), LocalDate.parse("2012-11-11")));
	}
}
