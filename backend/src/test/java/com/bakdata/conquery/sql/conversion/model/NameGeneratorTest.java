package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.SumSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorCteStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NameGeneratorTest {

	private static final int NAME_MAX_LENGTH = 63;

	@Test
	void cteStepName() {
		NameGenerator nameGenerator = new NameGenerator(NAME_MAX_LENGTH);

		Assertions.assertEquals(
				"foo-group_filter",
				nameGenerator.cteStepName(ConnectorCteStep.AGGREGATION_FILTER, "foo")
		);

		String veryLongCteName = "concept-foobar-fizzbuzz-123_aggregation_select_filter_date_aggregation_cte";
		String actual = nameGenerator.cteStepName(ConnectorCteStep.AGGREGATION_FILTER, veryLongCteName);
		Assertions.assertEquals(NAME_MAX_LENGTH, actual.length());
		Assertions.assertEquals("123_aggregation_select_filter_date_aggregation_cte-group_filter", actual);
	}

	@Test
	void selectName() {

		NameGenerator nameGenerator = new NameGenerator(NAME_MAX_LENGTH);

		SumSelect sumSelect = new SumSelect();
		sumSelect.setName("sum_select");
		Assertions.assertEquals("sum_select-1", nameGenerator.selectName(sumSelect));

		SumSelect secondSumSelect = new SumSelect();
		secondSumSelect.setName("sum_select");
		Assertions.assertEquals("sum_select-2", nameGenerator.selectName(secondSumSelect));

		SumFilter<?> sumFilter = new SumFilter<>();
		sumFilter.setName("sum_filter");
		Assertions.assertEquals("sum_filter-1", nameGenerator.selectName(sumFilter));

		SumSelect sumSelectWithTooLongName = new SumSelect();
		String veryLongSelectLabel = "First Last Random Date of Birth Aggregation Select Distinct By Minus Sum Count";
		sumSelectWithTooLongName.setName(veryLongSelectLabel);

		String actual = nameGenerator.selectName(sumSelectWithTooLongName);
		Assertions.assertEquals(NAME_MAX_LENGTH, actual.length());
		Assertions.assertEquals("_date_of_birth_aggregation_select_distinct_by_minus_sum_count-1", actual);
	}

	@Test
	void joinedNodeName() {
		NameGenerator nameGenerator = new NameGenerator(NAME_MAX_LENGTH);

		String firstAnd = nameGenerator.joinedNodeName(LogicalOperation.AND);
		Assertions.assertEquals("AND-1", firstAnd);

		String firstOr = nameGenerator.joinedNodeName(LogicalOperation.OR);
		Assertions.assertEquals("OR-1", firstOr);

		String secondAnd = nameGenerator.joinedNodeName(LogicalOperation.AND);
		Assertions.assertEquals("AND-2", secondAnd);

		String thirdAnd = nameGenerator.joinedNodeName(LogicalOperation.AND);
		Assertions.assertEquals("AND-3", thirdAnd);

		String secondOr = nameGenerator.joinedNodeName(LogicalOperation.OR);
		Assertions.assertEquals("OR-2", secondOr);

		String thirdOr = nameGenerator.joinedNodeName(LogicalOperation.OR);
		Assertions.assertEquals("OR-3", thirdOr);
	}

	@Test
	void conceptConnectorName() {

		NameGenerator nameGenerator = new NameGenerator(NAME_MAX_LENGTH);

		CQConcept foo = new CQConcept();
		foo.setLabel("foo");
		Assertions.assertEquals(
				"concept_foo-1",
				nameGenerator.conceptName(foo),
				"first concept, count is 1"
		);

		ConceptTreeConnector fooConnector = new ConceptTreeConnector();
		fooConnector.setName("foo connector");
		Assertions.assertEquals(
				"concept_foo_foo_connector-1",
				nameGenerator.conceptConnectorName(foo, fooConnector),
				"first concept, first connector, count is still 1"
		);

		ConceptTreeConnector barConnector = new ConceptTreeConnector();
		barConnector.setName("bar connector");
		Assertions.assertEquals(
				"concept_foo_bar_connector-1",
				nameGenerator.conceptConnectorName(foo, barConnector),
				"first concept, second connector, count is still 1"
		);

		CQConcept bar = new CQConcept();
		bar.setLabel("bar");
		Assertions.assertEquals(
				"concept_bar-2",
				nameGenerator.conceptName(bar),
				"second concept, count is 2"
		);
		Assertions.assertEquals(
				"concept_bar_bar_connector-2",
				nameGenerator.conceptConnectorName(bar, barConnector),
				"second concept, second connector, count is 2"
		);

		CQConcept withTooLongLabel = new CQConcept();
		String veryLongConceptLabel = "Verbose Concept label which exceeds the max length and nobody would ever choose I mean seriously";
		withTooLongLabel.setLabel(veryLongConceptLabel);
		ConceptTreeConnector connector = new ConceptTreeConnector();
		connector.setName("with-too-long-label-connector");
		Assertions.assertEquals(
				"_the_max_length_and_nobody_would_ever_choose_i_mean_seriously-3",
				nameGenerator.conceptName(withTooLongLabel),
				"third concept, name should be truncated"
		);
		Assertions.assertEquals(
				"ld_ever_choose_i_mean_seriously_with-too-long-label-connector-3",
				nameGenerator.conceptConnectorName(withTooLongLabel, connector),
				"label way too long, name should be truncated, counter still 3"
		);
	}

}
