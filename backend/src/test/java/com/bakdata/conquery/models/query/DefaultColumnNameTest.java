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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.SelectHolder;
import com.bakdata.conquery.models.concepts.conditions.EqualCondition;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeConnector;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.resultinfo.SelectResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import io.dropwizard.jersey.validation.Validators;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DefaultColumnNameTest {
	private final static DatasetRegistry DATASET_REGISTRY = mock(DatasetRegistry.class);
	private final static PrintSettings SETTINGS = new PrintSettings(false, Locale.ENGLISH, DATASET_REGISTRY);
	private final static Validator VALIDATOR = Validators.newValidator();
	
	private final static Function<TestConcept,Select> FIRST_CONCEPT_SELECT_EXTRACTOR = (concept) -> concept.getSelects().get(0);
	private final static Function<TestConcept,Select> FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR = (concept) -> concept.getConnectors().get(0).getSelects().get(0);
	
	private static Stream<Arguments> provideCombinations() {
		return Stream.of(
			// ConceptSelect, without CQLabel, one Id
			Arguments.of(
				TestConcept.create(1, FIRST_CONCEPT_SELECT_EXTRACTOR, 1),
				false,
				"TestConceptLabel - ID_0 - TestSelectLabel"),
			// ConceptSelect without CQLabel, multiple Ids
			Arguments.of(
				TestConcept.create(1, FIRST_CONCEPT_SELECT_EXTRACTOR, 3),
				false,
				"TestConceptLabel - ID_0+ID_1+ID_2 - TestSelectLabel"),
			// ConceptSelect with CQLabel, one Id
			Arguments.of(
				TestConcept.create(1, FIRST_CONCEPT_SELECT_EXTRACTOR, 1),
				true,
				"TestCQLabel - TestSelectLabel"),
			// ConceptSelect with CQLabel, multiple Ids
			Arguments.of(
				TestConcept.create(1, FIRST_CONCEPT_SELECT_EXTRACTOR, 3),
				true,
				"TestCQLabel - TestSelectLabel"),
			
			// ConnectorSelect, without CQLabel, one Id, one Connector
			Arguments.of(
				TestConcept.create(1, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 1),
				false,
				"TestConceptLabel - ID_0 - TestSelectLabel"),
			// ConnectorSelect without CQLabel, multiple Ids, one Connector
			Arguments.of(
				TestConcept.create(1, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 3),
				false,
				"TestConceptLabel - ID_0+ID_1+ID_2 - TestSelectLabel"),
			// ConnectorSelect with CQLabel, one Id, one Connector
			Arguments.of(
				TestConcept.create(1, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 1),
				true,
				"TestCQLabel - TestSelectLabel"),
			// ConnectorSelect with CQLabel, multiple Ids, one Connector
			Arguments.of(
				TestConcept.create(1, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 3),
				true,
				"TestCQLabel - TestSelectLabel"),
			
			// ConnectorSelect, without CQLabel, one Id, multiple Connectors
			Arguments.of(
				TestConcept.create(3, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 1),
				false,
				"TestConceptLabel - ID_0 - TestConnectorLabel_0 TestSelectLabel"),
			// ConnectorSelect without CQLabel, multiple Ids, multiple Connectors
			Arguments.of(
				TestConcept.create(3, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 3),
				false,
				"TestConceptLabel - ID_0+ID_1+ID_2 - TestConnectorLabel_0 TestSelectLabel"),
			// ConnectorSelect with CQLabel, one Id, multiple Connectors
			Arguments.of(
				TestConcept.create(3, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 1),
				true,
				"TestCQLabel - TestConnectorLabel_0 TestSelectLabel"),
			// ConnectorSelect with CQLabel, multiple Ids, multiple Connectors
			Arguments.of(
				TestConcept.create(3, FIRST_CONNECTOR_FIRST_SELECT_EXTRACTOR, 3),
				true,
				"TestCQLabel - TestConnectorLabel_0 TestSelectLabel")
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
		
		SelectResultInfo info = new SelectResultInfo(concept.extractSelect(), concept.createCQConcept(hasCQConceptLabel));
		
		assertThat(SETTINGS.columnName(info)).isEqualTo(expectedColumnName);
	}

	
	private static class TestCQConcept extends CQConcept {
		private static CQConcept create(boolean withLabel, TestConcept concept) {
			CQConcept cqConcept = new CQConcept();
			if(withLabel) {				
				cqConcept.setLabel("TestCQLabel");
			}


			cqConcept.setElements(
					concept.getChildren().stream()
							.sorted(Comparator.comparing(ctc -> ctc.getId().toString()))
							.collect(Collectors.toList())
			);

			cqConcept.setSelects(List.of(concept.extractSelect()));

			return cqConcept;
		}
	}
	
	private static class TestConcept extends TreeConcept {

		private static final DatasetId DATASET = new DatasetId("test");
		private final Function<TestConcept,Select> selectExtractor;
		
		private TestConcept(Function<TestConcept,Select> selectExtractor) {
			this.selectExtractor = selectExtractor;
			setName("TestConceptName");
			setLabel("TestConceptLabel");
			setDataset(DATASET);
			setSelects(List.of(new TestUniversalSelect(this)));
		}
		
		public Select extractSelect(){
			return selectExtractor.apply(this);
		}

		public CQConcept createCQConcept(boolean hasCQConceptLabel){
			return TestCQConcept.create(hasCQConceptLabel, this);
		}

		@SneakyThrows
		public static TestConcept create(int countConnectors, Function<TestConcept,Select> selectExtractor, int countIds) {
			TestConcept concept = new TestConcept(selectExtractor);
			ArrayList<ConceptTreeConnector> connectors = new ArrayList<>();
			concept.setConnectors(connectors);
			for (; countConnectors > 0; countConnectors--) {
				connectors.add(new TestConnector(concept));
			}

			ArrayList<ConceptTreeChild> children = new ArrayList<>();
			for (; countIds > 0; countIds--) {
				String childName = "ID_" + (countIds-1);
				ConceptTreeChild child = new ConceptTreeChild();
				child.setParent(concept);
				child.setName(childName);
				child.setCondition(new EqualCondition(Set.of(childName)));
				children.add(child);
			}

			concept.setChildren(children);
			concept.initElements(VALIDATOR);
			
			return concept;
		}
		
		@SuppressWarnings("serial")
		private static class TestConnector extends ConceptTreeConnector {
			
			public TestConnector(TreeConcept concept) {
				int presentConnectors = concept.getConnectors().size();
				setName("TestConnectorName_"+presentConnectors);
				setLabel("TestConnectorLabel_"+presentConnectors);
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
