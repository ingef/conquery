package com.bakdata.conquery.models.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.concepts.conditions.EqualCondition;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import io.dropwizard.jersey.validation.Validators;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class DefaultColumnNameTest {
	private static final DatasetRegistry DATASET_REGISTRY = mock(DatasetRegistry.class);
	private static final PrintSettings SETTINGS = new PrintSettings(false, Locale.ENGLISH, DATASET_REGISTRY, new ConqueryConfig(), null);
	private static final Validator VALIDATOR = Validators.newValidator();

	private static final BiFunction<TestConcept, CQConcept, Select> CONCEPT_SELECT_SELECTOR =
			(concept, cq) -> {
				final UniversalSelect select = concept.getSelects().get(0);
				cq.setSelects(List.of(select));
				return select;
			};

	private static final BiFunction<TestConcept, CQConcept, Select> CONNECTOR_SELECT_SELECTOR =
			(concept, cq) -> {
				final Select select = concept.getConnectors().get(0).getSelects().get(0);
				cq.getTables().get(0).setSelects(List.of(select));
				return select;
			};

	private static Stream<Arguments> provideCombinations() {
		return Stream.of(
				// ConceptSelect, without CQLabel, one Id
				Arguments.of(
						TestConcept.create(1, CONCEPT_SELECT_SELECTOR, 1, null),
						false,
						"TestConceptLabel - ID_0 - TestSelectLabel"
				),
				// ConceptSelect without CQLabel, multiple Ids
				Arguments.of(
						TestConcept.create(1, CONCEPT_SELECT_SELECTOR, 3, null),
						false,
						"TestConceptLabel - ID_0+ID_1+ID_2 - TestSelectLabel"
				),
				// ConceptSelect with CQLabel, one Id
				Arguments.of(
						TestConcept.create(1, CONCEPT_SELECT_SELECTOR, 1, null),
						true,
						"TestCQLabel - TestSelectLabel"
				),
				// ConceptSelect with CQLabel, multiple Ids
				Arguments.of(
						TestConcept.create(1, CONCEPT_SELECT_SELECTOR, 3, null),
						true,
						"TestCQLabel - TestSelectLabel"
				),

				// ConnectorSelect, without CQLabel, one Id, one Connector
				Arguments.of(
						TestConcept.create(1, CONNECTOR_SELECT_SELECTOR, 1, null),
						false,
						"TestConceptLabel - ID_0 - TestSelectLabel"
				),
				// ConnectorSelect without CQLabel, multiple Ids, one Connector
				Arguments.of(
						TestConcept.create(1, CONNECTOR_SELECT_SELECTOR, 3, null),
						false,
						"TestConceptLabel - ID_0+ID_1+ID_2 - TestSelectLabel"
				),
				// ConnectorSelect with CQLabel, one Id, one Connector
				Arguments.of(
						TestConcept.create(1, CONNECTOR_SELECT_SELECTOR, 1, null),
						true,
						"TestCQLabel - TestSelectLabel"
				),
				// ConnectorSelect with CQLabel, multiple Ids, one Connector
				Arguments.of(
						TestConcept.create(1, CONNECTOR_SELECT_SELECTOR, 3, null),
						true,
						"TestCQLabel - TestSelectLabel"
				),

				// ConnectorSelect, without CQLabel, one Id, multiple Connectors
				Arguments.of(
						TestConcept.create(3, CONNECTOR_SELECT_SELECTOR, 1, null),
						false,
						"TestConceptLabel - ID_0 - TestConnectorLabel_0 TestSelectLabel"
				),
				// ConnectorSelect without CQLabel, multiple Ids, multiple Connectors
				Arguments.of(
						TestConcept.create(3, CONNECTOR_SELECT_SELECTOR, 3, null),
						false,
						"TestConceptLabel - ID_0+ID_1+ID_2 - TestConnectorLabel_0 TestSelectLabel"
				),
				// ConnectorSelect with CQLabel, one Id, multiple Connectors
				Arguments.of(
						TestConcept.create(3, CONNECTOR_SELECT_SELECTOR, 1, null),
						true,
						"TestCQLabel - TestConnectorLabel_0 TestSelectLabel"
				),
				// ConnectorSelect with CQLabel, multiple Ids, multiple Connectors
				Arguments.of(
						TestConcept.create(3, CONNECTOR_SELECT_SELECTOR, 3, null),
						true,
						"TestCQLabel - TestConnectorLabel_0 TestSelectLabel"
				),
				// ConnectorSelect without CQLabel, only root Id, one Connector -> Connector label should be suppressed
				Arguments.of(
						TestConcept.create(1, CONNECTOR_SELECT_SELECTOR, 0, "Test-Label"),
						false,
						"Test-Label - TestSelectLabel"
				)

		);
	}

	@ParameterizedTest
	@MethodSource("provideCombinations")
	void checkCombinations(TestConcept concept, boolean hasCQConceptLabel, String expectedColumnName) {

		doAnswer(invocation -> {
			final ConceptId id = invocation.getArgument(0);
			if (!concept.getId().equals(id)) {
				throw new IllegalStateException("Expected the id " + concept.getId() + " but got " + id);
			}
			return concept;
		}).when(DATASET_REGISTRY).resolve(any());

		final CQConcept cqConcept = concept.createCQConcept(hasCQConceptLabel);

		SelectResultInfo info = new SelectResultInfo(concept.extractSelect(cqConcept), cqConcept);

		assertThat(info.getUniqueName(SETTINGS)).isEqualTo(expectedColumnName);
	}


	private static class TestCQConcept extends CQConcept {
		private static CQConcept create(boolean withLabel, TestConcept concept) {
			CQConcept cqConcept = new CQConcept();
			if (withLabel) {
				cqConcept.setLabel("TestCQLabel");
			}


			List<ConceptElement<?>> elements = concept.getChildren().stream()
													  .sorted(Comparator.comparing(ctc -> ctc.getId().toString()))
													  .collect(Collectors.toList());

			if (elements.isEmpty()) {
				elements = List.of(concept);
			}
			cqConcept.setElements(
					elements
			);

			List<CQTable> tables = concept.getConnectors().stream()
										  .map(con -> {
											  CQTable table = new CQTable();
											  table.setConnector(con);
											  table.setConcept(cqConcept);
											  return table;
										  })
										  .collect(Collectors.toList());
			cqConcept.setTables(tables);

			concept.extractSelect(cqConcept);

			ValidatorHelper.failOnError(log, VALIDATOR.validate(cqConcept));
			return cqConcept;
		}
	}

	private static class TestConcept extends TreeConcept {

		private static final Dataset DATASET = new Dataset() {
			{
				setName("test");
			}
		};
		private final BiFunction<TestConcept, CQConcept, Select> selectExtractor;

		private TestConcept(BiFunction<TestConcept, CQConcept, Select> selectExtractor) {
			this.selectExtractor = selectExtractor;
			setName("TestConceptName");
			setLabel("TestConceptLabel");
			setDataset(DATASET);
			setSelects(List.of(new TestUniversalSelect(this)));
		}

		public Select extractSelect(CQConcept cq) {
			return selectExtractor.apply(this, cq);
		}

		public CQConcept createCQConcept(boolean hasCQConceptLabel) {
			return TestCQConcept.create(hasCQConceptLabel, this);
		}


		@SneakyThrows
		public static TestConcept create(int countConnectors, BiFunction<TestConcept, CQConcept, Select> selectExtractor, int countIds, String overwriteLabel) {
			TestConcept concept = new TestConcept(selectExtractor);
			if (overwriteLabel != null) {
				concept.setLabel(overwriteLabel);
			}
			List<ConceptTreeConnector> connectors = new ArrayList<>();
			concept.setConnectors(connectors);
			for (; countConnectors > 0; countConnectors--) {
				TestConnector con = new TestConnector(concept);
				if (overwriteLabel != null) {
					con.setLabel(overwriteLabel);
				}
				connectors.add(con);
			}

			List<ConceptTreeChild> children = new ArrayList<>();
			for (; countIds > 0; countIds--) {
				String childName = "ID_" + (countIds - 1);
				ConceptTreeChild child = new ConceptTreeChild();
				child.setParent(concept);
				child.setName(childName);
				if (overwriteLabel != null) {
					child.setLabel(overwriteLabel);
				}
				child.setCondition(new EqualCondition(Set.of(childName)));
				children.add(child);
			}

			concept.setChildren(children);
			concept.initElements(VALIDATOR);

			return concept;
		}

		private static class TestConnector extends ConceptTreeConnector {

			public TestConnector(TreeConcept concept) {
				int presentConnectors = concept.getConnectors().size();
				setName("TestConnectorName_" + presentConnectors);
				setLabel("TestConnectorLabel_" + presentConnectors);
				setConcept(concept);
				setSelects(List.of(new TestUniversalSelect(this)));
			}

		}


		private static class TestUniversalSelect extends UniversalSelect {

			public TestUniversalSelect(SelectHolder<?> holder) {
				setName("TestSelectName");
				setLabel("TestSelectLabel");
				setHolder(holder);
			}

			@Override
			public Aggregator<?> createAggregator() {
				return null;
			}
		}

	}
}
