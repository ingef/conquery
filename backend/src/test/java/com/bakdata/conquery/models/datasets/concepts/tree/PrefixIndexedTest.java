//package com.bakdata.conquery.models.datasets.concepts.tree;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.io.IOException;
//import java.util.Collections;
//import java.util.Map;
//import java.util.stream.Stream;
//
//import com.bakdata.conquery.io.jackson.Jackson;
//import com.bakdata.conquery.models.datasets.concepts.Concept;
//import com.bakdata.conquery.models.datasets.Column;
//import com.bakdata.conquery.models.datasets.Dataset;
//import com.bakdata.conquery.models.datasets.Table;
//import com.bakdata.conquery.models.exceptions.ConfigurationException;
//import com.bakdata.conquery.models.exceptions.JSONException;
//import com.bakdata.conquery.models.identifiable.CentralRegistry;
//import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
//import com.bakdata.conquery.models.events.MajorTypeId;
//import com.bakdata.conquery.models.events.stores.specific.string.StringTypeDictionary;
//import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
//import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded.Encoding;
//import com.bakdata.conquery.models.events.stores.specific.integer.VarIntTypeInt;
//import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
//import com.bakdata.conquery.util.CalculatedValue;
//import com.bakdata.conquery.util.dict.SuccinctTrie;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.github.powerlibraries.io.In;
//import io.dropwizard.jersey.validation.Validators;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
//@Slf4j
//public class PrefixIndexedTest {
//
//	private static final String CONCEPT_SOURCE = "prefixes.concept.json";
//
//	private static SuccinctTrie dict;
//	private static TreeConcept indexedConcept;
//	private static TreeConcept oldConcept;
//	private static ImportId importId;
//	private static StringTypeEncoded type;
//
//
//	public static Stream<Arguments> getTestKeys() {
//		CalculatedValue<Map<String, Object>> rowMap = new CalculatedValue<>(Collections::emptyMap);
//
//		return Stream.of(
//				"A047b" , "A0470" , "A0471" , "A0472" , "A0473" , "A0479" , "A3180" , "A3188" , "A4151" , "A4152" , "A4158" , "B2580" , "B2588" , "B3781" , "B3788" , "B9541" , "B9542" , "B9548" , "B9590" , "B9591" , "B9681" , "B9688" , "C4101" , "C4102" , "C4130" , "C4131" , "C4132" , "C7981" , "C7982" , "C7983" , "C7984" , "O029" , "Z935" , "M1202" , "E011" , "D370" , "P13" , "G259" , "J9921" , "I831" , "H950" , "E8331" , "H511" , "I11" , "S252" , "M0704"
//		)
//					.map(key -> Arguments.of(key, rowMap));
//	}
//
//
//	@BeforeAll
//	public static void init() throws IOException, JSONException, ConfigurationException {
//		dict = new SuccinctTrie();
//
//		getTestKeys()
//			.map(args -> (String) args.get()[0])
//			.map(String::getBytes)
//			.forEach(dict::add);
//
//		dict.compress();
//
//		ObjectNode node = Jackson.MAPPER.readerFor(ObjectNode.class).readValue(In.resource(PrefixIndexedTest.class, CONCEPT_SOURCE).asStream());
//
//		// load concept tree from json
//		CentralRegistry registry = new CentralRegistry();
//
//
//		Table table = new Table();
//
//		table.setName("the_table");
//		Dataset dataset = new Dataset();
//
//		dataset.setName("the_dataset");
//
//		registry.register(dataset);
//
//		table.setDataset(dataset);
//
//		Column column = new Column();
//		column.setName("the_column");
//		column.setType(MajorTypeId.STRING);
//
//		table.setColumns(new Column[]{column});
//		column.setTable(table);
//
//		registry.register(table);
//		registry.register(column);
//
//		importId = new ImportId(table.getId(), "import");
//
//		// load tree twice to to avoid references
//
//		indexedConcept = new SingletonNamespaceCollection(registry).injectInto(dataset.injectInto(Jackson.MAPPER.readerFor(Concept.class))).readValue(node);
//
//		indexedConcept.setDataset(dataset.getId());
//		indexedConcept.initElements(Validators.newValidator());
//
//
//		TreeChildPrefixIndex.putIndexInto(indexedConcept);
//		type = new StringTypeEncoded(new StringTypeDictionary(new VarIntTypeInt(-1, +1, delegate)), Encoding.UTF8);
//		type.getSubType().setDictionary(dict);
//		indexedConcept.initializeIdCache(type, importId);
//
//		oldConcept = new SingletonNamespaceCollection(registry).injectInto(dataset.injectInto(Jackson.MAPPER.readerFor(Concept.class))).readValue(node);
//
//		oldConcept.setDataset(dataset.getId());
//		oldConcept.initElements(Validators.newValidator());
//
//		assertThat(indexedConcept.getChildIndex()).isNotNull();
//		assertThat(oldConcept.getChildIndex()).isNull();
//	}
//
//	@ParameterizedTest(name = "{index}: {0}")
//	@MethodSource("getTestKeys")
//	public void basic(String key, CalculatedValue<Map<String, Object>> rowMap) throws JSONException {
//		log.trace("Searching for {}", key);
//
//		ConceptTreeChild idxResult = indexedConcept.findMostSpecificChild(key, rowMap);
//		ConceptTreeChild oldResult = oldConcept.findMostSpecificChild(key, rowMap);
//
//		if(oldResult == null) {
//			assertThat(idxResult).isNull();
//		}
//		else {
//			log.trace("index_result: {}", idxResult.getId());
//			log.trace("normal_result: {}", oldResult.getId());
//			assertThat(oldResult.getId())
//					.isEqualTo(idxResult.getId());
//		}
//	}
//
//	@ParameterizedTest
//	@MethodSource("getTestKeys")
//	public void withTail(String key, CalculatedValue<Map<String, Object>> rowMap) throws JSONException {
//		String keyWithTail = key + ".someTextAfterTheActualText";
//
//		log.trace("Searching for {}", key);
//
//		ConceptTreeChild idxResultTail = indexedConcept.findMostSpecificChild(keyWithTail, rowMap);
//		ConceptTreeChild idxResult = indexedConcept.findMostSpecificChild(key, rowMap);
//
//		assertThat(idxResult.getId()).isEqualTo(idxResultTail.getId());
//	}
//
//	@ParameterizedTest
//	@MethodSource("getTestKeys")
//	public void cached(String key, CalculatedValue<Map<String, Object>> rowMap) throws JSONException {
//		log.trace("Searching for {}", key);
//
//		ConceptTreeChild reference = indexedConcept.findMostSpecificChild(key, rowMap);
//		ConceptTreeChild cached = indexedConcept.getCache(importId).findMostSpecificChild(type.getId(key), type.getElement(type.getId(key)), rowMap);
//		ConceptTreeChild cached2 = indexedConcept.getCache(importId).findMostSpecificChild(type.getId(key), type.getElement(type.getId(key)), rowMap);
//
//		assertThat(reference.getId())
//				.describedAs("%s hierarchical name", key)
//				.isEqualTo(cached2.getId())
//				.isEqualTo(cached.getId())
//		;
//	}
//
//
//	@Test
//	public void missing() throws JSONException {
//		final String key = "valueThatIsNotPresent";
//
//		log.trace("Searching for Missing value {}", key);
//
//		ConceptTreeChild idxResult = indexedConcept.findMostSpecificChild(key, new CalculatedValue<>(Collections::emptyMap));
//
//		assertThat(idxResult).isNull();
//	}
//}