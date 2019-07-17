package com.bakdata.conquery.util.dict;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.powerlibraries.io.In;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SuccinctTrieTest {

	public static Stream<String> data() throws IOException {
		return In.resource(SuccinctTrieTest.class, "SuccinctTrieTest.data").streamLines();
	}

	@Test
	public void replicationTest() throws IOException {
		SuccinctTrie dict = new SuccinctTrie();
		DirectDictionary direct = new DirectDictionary(dict);
		dict.setDataset(new DatasetId("dataset"));
		dict.setName("dict");

		data().forEach(direct::put);

		dict.compress();

		SuccinctTrie replicatedDict = SuccinctTrie.fromSerialized(dict.toSerialized());
		
		assertThat(IntStream.range(0, dict.size())).allSatisfy(id -> {
			assertThat(replicatedDict.getElement(id)).isEqualTo(dict.getElement(id));
		});
		
	}

	@Test
	public void assertionTest() {
		List<String> words = new ArrayList<String>();
		words.add("hat");
		words.add("it");
		words.add("is");
		words.add("a");
		words.add("is");
		words.add("ha");
		words.add("hat");

		SuccinctTrie dict = new SuccinctTrie();
		DirectDictionary direct = new DirectDictionary(dict);

		int distinctValues = 0;
		for (String entry : words) {
			int id = direct.put(entry);
			if (id > distinctValues) {
				distinctValues++;
			}
		}

		dict.compress();
		
		assertThat(direct.getElement(0)).isEqualTo("hat");
		assertThat(direct.getElement(1)).isEqualTo("it");
		assertThat(direct.getElement(2)).isEqualTo("is");
		assertThat(direct.getElement(3)).isEqualTo("a");
		assertThat(direct.getId("is")).isEqualTo(2);
		assertThat(direct.getId("ha")).isEqualTo(4);
		assertThat(direct.getId("h")).isEqualTo(-1);
	}

	@Test
	public void serializationTest()
			throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {

		SuccinctTrie dict = new SuccinctTrie();
		DirectDictionary direct = new DirectDictionary(dict);
		dict.setDataset(new DatasetId("test"));
		dict.setName("testDict");
		data().forEach(direct::put);

		dict.compress();
		SerializationTestUtil
			.forType(Dictionary.class)
			.test(dict);
	}
	
	public static long[] getSeeds() {
		return new long[] {0L, 7L};
	}
	
	@ParameterizedTest(name="seed: {0}")
	@MethodSource("getSeeds")
	public void valid(long seed) {
		final SuccinctTrie dict = new SuccinctTrie();
		DirectDictionary direct = new DirectDictionary(dict);
		final BiMap<String, Integer> reference = HashBiMap.create();

		AtomicInteger count = new AtomicInteger(0);

		Random random = new Random(seed);

		IntStream
			.range(0, 8192)
			.boxed()
			.sorted(TernaryTreeTestUtil.shuffle(random))
			.forEach( rep -> {
				final String prefix = Integer.toString(rep, 26);
	
				reference.put(prefix, count.get());
				dict.add(prefix.getBytes());
				count.incrementAndGet();
			});
		
		log.info("structure build");
		dict.compress();
		log.info("trie compressed");
		//assert key value lookup
		assertThat(reference.entrySet().stream()).allSatisfy(entry -> {
			assertThat(direct.getId(entry.getKey()))
				.isEqualTo(entry.getValue());
		});
		
		log.info("forward lookup done");

		//assert reverse lookup
		assertThat(reference.inverse().entrySet().stream()).allSatisfy(entry -> {
			assertThat(dict.getElement(entry.getKey()))
				.isEqualTo(entry.getValue().getBytes());
		});
		log.info("reverse lookup done");
	}
}