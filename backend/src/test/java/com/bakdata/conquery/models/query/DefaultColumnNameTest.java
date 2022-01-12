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

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.concepts.conditions.EqualCondition;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
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

	private static BiFunction<TestConcept, CQConcept, Select> conceptSelectGenerator(String selectName) {
		return (concept, cq) -> {

			final UniversalSelect select = new TestUniversalSelect(concept, selectName);
			cq.setSelects(List.of(select));
			return select;
		};
	}

	private static BiFunction<TestConcept, CQConcept, Select> connectorSelectGenerator(int connector, String selectName) {
		return (concept, cq) -> {

			final UniversalSelect select = new TestUniversalSelect(concept.getConnectors().get(connector), selectName);
			cq.getTables().get(connector).setSelects(List.of(select));
			return select;
		};
	}

	private static Stream<Arguments> provideCombinations() {
		return Stream.of(
				// Exactly the same names on every level (ConceptSelect)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, conceptSelectGenerator("Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, conceptSelectGenerator("Select"))
						),
						false,
						List.of(
								"Concept ID_0 Select",
								"Concept ID_0 Select_1"
						)
				),
				// Exactly the same names on every level (ConnectorSelect, single connector)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept ID_0 Select",
								"Concept ID_0 Select_1"
						)
				),
				// Exactly the same names on every level (ConnectorSelect, multiple connectors)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector1", "Connector2"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector1", "Connector2"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept ID_0 Connector1 Select",
								"Concept ID_0 Connector1 Select_1"
						)
				),
				// Concept label differs (ConceptSelect)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, conceptSelectGenerator("Select")),
								TestConcept.create("Concept2", new String[]{"ID_0"}, new String[]{"Connector"}, conceptSelectGenerator("Select"))
						),
						false,
						List.of(
								"Concept Select",
								"Concept2 Select"
						)
				),
				// Concept label differs (ConnectorSelect, single connector)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept2", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept Select",
								"Concept2 Select"
						)
				),
				// Concept label differs (ConnectorSelect, multiple connectors)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector1", "Connector2"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept2", new String[]{"ID_0"}, new String[]{"Connector1", "Connector2"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept Select",
								"Concept2 Select"
						)
				),
				// ID label differs (ConceptSelect)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, conceptSelectGenerator("Select")),
								TestConcept.create("Concept", new String[]{"ID_1"}, new String[]{"Connector"}, conceptSelectGenerator("Select"))
						),
						false,
						List.of(
								"Concept ID_0 Select",
								"Concept ID_1 Select"
						)
				),
				// ID label differs (ConnectorSelect, single connector)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_1"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept ID_0 Select",
								"Concept ID_1 Select"
						)
				),
				// ID label differs/multiple (ConnectorSelect, single connector)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0", "ID_1"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_1"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept ID_0+ID_1 Select",
								"Concept ID_1 Select"
						)
				),
				// Connector label differs (ConnectorSelect, single connector), because each concept has only one connector it is skipped
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector1"}, connectorSelectGenerator(0, "Select"))
						),
						false,
						List.of(
								"Concept ID_0 Select",
								"Concept ID_0 Select_1"
						)
				),
				// Connector label differs (ConnectorSelect, multiple connectors)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, connectorSelectGenerator(1, "Select"))
						),
						false,
						List.of(
								"Concept ID_0 Connector Select",
								"Concept ID_0 Connector1 Select"
						)
				),
				// Select label differs (ConceptSelect)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, conceptSelectGenerator("Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, conceptSelectGenerator("OtherSelect"))
						),
						false,
						List.of(
								"Select",
								"OtherSelect"
						)
				),
				// Select label differs (ConnectorSelect, multiple connectors)
				Arguments.of(
						List.of(
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, connectorSelectGenerator(0, "Select")),
								TestConcept.create("Concept", new String[]{"ID_0"}, new String[]{"Connector","Connector1"}, connectorSelectGenerator(0, "OtherSelect"))
						),
						false,
						List.of(
								"Select",
								"OtherSelect"
						)
				)

		);
	}

	@ParameterizedTest
	@MethodSource("provideCombinations")
	void checkCombinations(List<TestConcept> concepts, boolean hasCQConceptLabel, List<String> expectedColumnNames) {

		final List<ResultInfo> infos = new ArrayList<>();

		for (TestConcept concept : concepts) {
			// Mock dataset registry to resolve ids
			doAnswer(invocation -> {
				final ConceptId id = invocation.getArgument(0);
				if (!concept.getId().equals(id)) {
					throw new IllegalStateException("Expected the id " + concept.getId() + " but got " + id);
				}
				return concept;
			}).when(DATASET_REGISTRY).resolve(any());

			final CQConcept cqConcept = concept.createCQConcept(hasCQConceptLabel);


			infos.add(new SelectResultInfo(concept.extractSelect(cqConcept), cqConcept));
		}

		final UniqueNamer uniqNamer = new UniqueNamer(SETTINGS, infos);

		final List<String> actual = infos.stream().map(uniqNamer::getUniqueName).collect(Collectors.toList());

		assertThat(actual).containsExactlyElementsOf(expectedColumnNames);
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

		private TestConcept(String conceptLabel, BiFunction<TestConcept, CQConcept, Select> selectExtractor) {
			this.selectExtractor = selectExtractor;
			setLabel(conceptLabel);
			setDataset(DATASET);
			validator = VALIDATOR;

		}

		public Select extractSelect(CQConcept cq) {
			return selectExtractor.apply(this, cq);
		}

		public CQConcept createCQConcept(boolean hasCQConceptLabel) {
			return TestCQConcept.create(hasCQConceptLabel, this);
		}


		@SneakyThrows
		public static TestConcept create(
				String conceptLabel,
				String[] ids,
				String[] connectorNames,
				BiFunction<TestConcept, CQConcept, Select> selectExtractor
		) {
			TestConcept concept = new TestConcept(conceptLabel, selectExtractor);
			List<ConceptTreeConnector> connectors = new ArrayList<>();
			concept.setConnectors(connectors);
			for (String connectorName : connectorNames) {
				TestConnector con = new TestConnector(concept);
				con.setLabel(connectorName);
				connectors.add(con);
			}

			List<ConceptTreeChild> children = new ArrayList<>();
			for (String id : ids) {
				ConceptTreeChild child = new ConceptTreeChild();
				child.setParent(concept);
				child.setLabel(id);
				child.setCondition(new EqualCondition(Set.of(id)));
				children.add(child);
			}

			concept.setChildren(children);
			concept.initElements();

			return concept;
		}

		private static class TestConnector extends ConceptTreeConnector {

			public TestConnector(TreeConcept concept) {
				int presentConnectors = concept.getConnectors().size();
				setLabel("TestConnectorLabel_" + presentConnectors);
				setConcept(concept);
			}

		}


	}

	private static class TestUniversalSelect extends UniversalSelect {

		public TestUniversalSelect(SelectHolder<?> holder, String name) {
			setLabel(name);
			setHolder(holder);
		}

		@Override
		public Aggregator<?> createAggregator() {
			return null;
		}
	}
}
