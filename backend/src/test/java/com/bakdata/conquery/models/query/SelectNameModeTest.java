package com.bakdata.conquery.models.query;

import static com.bakdata.conquery.models.query.DefaultColumnNameTest.*;
import static com.bakdata.conquery.models.query.resultinfo.SelectResultInfo.SelectNameMode.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SelectNameModeTest {

	// Connector Select
	public static final TestConcept
			CONCEPT_CONN_SEL =
			TestConcept.create("Concept", new String[]{"CHILD", "CHILD1"}, new String[]{"Connector", "Connector1"}, connectorSelectGenerator(0, "Select"));
	public static final CQConcept CQ_CONCEPT_CONN_SEL = CONCEPT_CONN_SEL.createCQConcept(false);
	public static final SelectResultInfo INFO_CONN_SEL = new SelectResultInfo(CONCEPT_CONN_SEL.extractSelect(CQ_CONCEPT_CONN_SEL), CQ_CONCEPT_CONN_SEL);

	public static final TestConcept
			CONCEPT_CONC_SEL =
			TestConcept.create("Concept", new String[]{"CHILD", "CHILD1"}, new String[]{"Connector", "Connector1"}, conceptSelectGenerator("Select"));
	public static final CQConcept CQ_CONCEPT_CONC_SEL = CONCEPT_CONC_SEL.createCQConcept(false);
	public static final SelectResultInfo INFO_CONC_SEL = new SelectResultInfo(CONCEPT_CONC_SEL.extractSelect(CQ_CONCEPT_CONC_SEL), CQ_CONCEPT_CONC_SEL);

	public static final PrintSettings PRINT_SETTINGS = new PrintSettings(true, Locale.ROOT, null, new ConqueryConfig(), null);

	private static Stream<Arguments> provideCombinations() {
		return Stream.of(
				Arguments.of(SELECT, INFO_CONN_SEL, "Select"),
				Arguments.of(CONCEPT_SELECT, INFO_CONN_SEL, "Concept Select"),
				Arguments.of(CONCEPT_CHILDREN_SELECT, INFO_CONN_SEL, "Concept CHILD+CHILD1 Select"),
				Arguments.of(CONCEPT_CHILDREN_CONNECTOR_SELECT, INFO_CONN_SEL, "Concept CHILD+CHILD1 Connector Select"),

				Arguments.of(SELECT, INFO_CONC_SEL, "Select"),
				Arguments.of(CONCEPT_SELECT, INFO_CONC_SEL, "Concept Select"),
				Arguments.of(CONCEPT_CHILDREN_SELECT, INFO_CONC_SEL, "Concept CHILD+CHILD1 Select"),
				Arguments.of(CONCEPT_CHILDREN_CONNECTOR_SELECT, INFO_CONC_SEL, "Concept CHILD+CHILD1 Select")
		);
	}


	@ParameterizedTest(name = ParameterizedTest.ARGUMENTS_PLACEHOLDER)
	@MethodSource("provideCombinations")
	public void selectLabel(SelectResultInfo.SelectNameMode mode, SelectResultInfo info, String expected){
		final String label = mode.defaultColumnName(PRINT_SETTINGS, info);
		assertThat(label).isEqualTo(expected);
	}
}
