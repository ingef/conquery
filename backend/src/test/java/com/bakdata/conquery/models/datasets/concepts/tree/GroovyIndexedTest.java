package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.util.CalculatedValue;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.TestNamespacedStorageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j @Execution(ExecutionMode.SAME_THREAD)
public class GroovyIndexedTest {

	public static final int SEED = 500;
	private static final String CONCEPT_SOURCE = "groovy.concept.json";
	private static TreeConcept indexedConcept;
	private static TreeConcept oldConcept;

	public static Stream<Arguments> getTestKeys() {
		final Random random = new Random(SEED);
		Supplier<CalculatedValue<Map<String, Object>>> rowMap = () -> new CalculatedValue<>(
				() -> Collections.singletonMap("distinction", 8 + random.nextInt(10))
		);

		return Stream.of(
							 "A13B", "I43A", "H41B", "B05Z", "L02C", "L12Z", "H08A", "I56B", "I03A", "E79C", "B80Z", "I47A", "N13A", "G08B", "F43B", "P04A", "T36Z", "T36Z", "N11A", "D13A", "R01D", "F06A", "F24A", "O03Z", "P01Z", "R63D", "A13A", "O05A", "G29B", "I18A", "J08A", "E74Z", "D06C", "H36Z", "H05Z", "P65B", "I09A", "A66Z", "F12E", "Q60E", "I46B", "I97Z", "I78Z", "T01B", "J24C", "A62Z", "Q01Z", "N25Z", "A01B", "G02A"
							 , "ZULU" // This may not fail, but return null on both sides
					 )
					 .map(v -> Arguments.of(v, rowMap.get()));
	}

	@BeforeAll
	public static void init() throws IOException, JSONException, ConfigurationException {
		final ObjectMapper mapper = Jackson.copyMapperAndInjectables(Jackson.MAPPER);
		ObjectNode node = mapper.readerFor(ObjectNode.class).readValue(In.resource(GroovyIndexedTest.class, CONCEPT_SOURCE).asStream());

		// load concept tree from json
		final NamespaceStorage storage = new NonPersistentStoreFactory().createNamespaceStorage();
		final NamespacedStorageProvider storageProvider = new TestNamespacedStorageProvider(storage);

		storage.openStores(mapper);
		Table table = new Table();

		table.setName("the_table");
		Dataset dataset = new Dataset("the_dataset");
		dataset.setStorageProvider(storageProvider);

		dataset.injectInto(mapper);

		storage.updateDataset(dataset);

		table.setNamespacedStorageProvider(storage);
		table.init();

		Column column = new Column();
		column.setName("the_column");
		column.setType(MajorTypeId.STRING);

		table.setColumns(new Column[]{column});
		column.setTable(table);

		storage.addTable(table);

		// Prepare Serdes injections
		final Validator validator = Validators.newValidator();
		final ObjectReader conceptReader = ((Injectable) values -> values.add(Validator.class, validator)).injectInto(mapper).readerFor(Concept.class);

		// load tree twice to to avoid references
		indexedConcept = conceptReader.readValue(node);

		indexedConcept.setNamespacedStorageProvider(storage);
		indexedConcept.initElements();

		oldConcept = conceptReader.readValue(node);

		oldConcept.setNamespacedStorageProvider(storage);
		oldConcept.initElements();
	}


	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("getTestKeys")
	public void basic(String key, CalculatedValue<Map<String, Object>> rowMap) throws JSONException {
		log.trace("Searching for {}", key);

		ConceptTreeChild idxResult = indexedConcept.findMostSpecificChild(key, rowMap);
		ConceptTreeChild oldResult = oldConcept.findMostSpecificChild(key, rowMap);

		assertThat(oldResult.getId()).describedAs("%s hierarchical name", key).isEqualTo(idxResult.getId());
	}


}