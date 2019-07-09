package com.bakdata.conquery.models.concepts.tree.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.tree.GroovyIndexedTest;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.In;
import com.google.common.base.Stopwatch;

import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag("slow")
@Disabled //see #179  Find fix for surefire plugin not understanding tags
public abstract class AbstractSearchPerformanceTest<QUERY_TYPE>{

	protected TreeConcept referenceConcept;

	protected TreeConcept newConcept;
	protected CentralRegistry registry;

	protected ImportId importId;

	private Dataset dataset;


	public abstract List<QUERY_TYPE> getTestKeys();
	public abstract String getName();

	public abstract void referenceSearch(QUERY_TYPE key) throws ConceptConfigurationException ;
	public abstract void newSearch(QUERY_TYPE key) throws ConceptConfigurationException ;

	public abstract String getConceptSourceName();
	public void postprocessConcepts() { }

	public int[] getIterations(){
		return new int[]{1000, 1000, 10000,50000};
	}

	@BeforeEach
	public void init() throws IOException, JSONException, ConfigurationException {
		// load concept tree from json
		initializeDataset();

		// load and assemble tree
		initializeConcepts();
	}

	public void initializeConcepts() throws IOException, ConfigurationException, JSONException {
		ObjectNode node = Jackson.MAPPER.readerFor(ObjectNode.class)
										.readValue(In.resource(GroovyIndexedTest.class, getConceptSourceName()).asStream());


		// load old tree concept before altering it to force loading of indexed concept.
		referenceConcept = new SingletonNamespaceCollection(registry).injectInto(dataset.injectInto(Jackson.MAPPER.readerFor(Concept.class)))
								.readValue(node);

		referenceConcept.setDataset(dataset.getId());
		referenceConcept.initElements(Validators.newValidator());

		newConcept = new SingletonNamespaceCollection(registry).injectInto(dataset.injectInto(Jackson.MAPPER.readerFor(Concept.class))).readValue(node);
		newConcept.setDataset(dataset.getId());
		newConcept.initElements(Validators.newValidator());

		postprocessConcepts();
	}


	public void initializeDataset() {
		registry = new CentralRegistry();

		Table table = new Table();

		table.setName("the_table");
		dataset = new Dataset();

		dataset.setName("the_dataset");

		registry.register(dataset);

		table.setDataset(dataset);

		importId = new ImportId(table.getId(), "import");

		Column column = new Column();
		column.setName("the_column");
		column.setType(MajorTypeId.STRING);

		table.setColumns(new Column[]{column});
		column.setTable(table);

		registry.register(table);
		registry.register(column);
	}

	//@TestFactory
	// //see #180  Re-enable these tests properly
	public Stream<DynamicTest> createTests(){
		return Arrays
			.stream(getIterations())
			.mapToObj(iter ->
				dynamicTest(
					String.format("%s, %d iterations", getName(), iter),
					() -> compareExecutionSpeed(iter)
				)
		);
	}

	public void compareExecutionSpeed(int iterations) throws JSONException {
		// Test Indexed Tree and gather results

		List<QUERY_TYPE> queries = getTestKeys();

		Duration newDuration;
		{
			Stopwatch stopwatch = Stopwatch.createStarted();

			for (int iteration = 0; iteration < iterations; iteration++) {
				Collections.shuffle(queries);

				for (QUERY_TYPE key : queries) {
					newSearch(key);
				}
			}

			stopwatch.stop();
			newDuration = stopwatch.elapsed();

			log.info("Duration for New Search: {} queries, {}", iterations, newDuration);
		}


		// Test Old implementation and gather results
		Duration referenceDuration;
		{
			Stopwatch stopwatch = Stopwatch.createStarted();

			for (int iteration = 0; iteration < iterations; iteration++) {
				Collections.shuffle(queries);

				for (QUERY_TYPE key : queries) {
					referenceSearch(key);
				}
			}

			stopwatch.stop();
			referenceDuration = stopwatch.elapsed();

			log.info("Full Duration for Normal Tree: {} queries, {}", iterations, referenceDuration);
		}

		assertThat(newDuration).isLessThan(referenceDuration);

		log.info("New took {}, Reference took {}", newDuration, referenceDuration);

	}
}

