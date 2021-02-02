package com.bakdata.conquery.util.dict;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.io.jackson.serializer.SerializationTestUtil;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.powerlibraries.io.In;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class SuccinctTrieTest {

	public static long[] getSeeds() {
		return new long[]{0L, 7L};
	}

	@Test
	public void replicationTest() throws IOException {
		SuccinctTrie dict = new SuccinctTrie(new DatasetId("dataset"), "name");
		MapDictionary direct = new MapDictionary(new DatasetId("dataset"), "name2");

		data().forEach(entry -> direct.put(entry.getBytes()));

		dict.compress();

		SuccinctTrie replicatedDict = SuccinctTrie.fromSerialized(dict.toSerialized());

		assertThat(IntStream.range(0, dict.size())).allSatisfy(id -> {
			assertThat(replicatedDict.getElement(id)).isEqualTo(dict.getElement(id));
		});

	}

	public static Stream<String> data() throws IOException {
		return In.resource(SuccinctTrieTest.class, "SuccinctTrieTest.data").streamLines();
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

		SuccinctTrie direct = new SuccinctTrie(new DatasetId("dataset"), "name");


		int distinctValues = 0;
		for (String entry : words) {
			int id = direct.put(entry.getBytes());
			if (id > distinctValues) {
				distinctValues++;
			}
		}

		direct.compress();

		assertThat(direct.getElement(0)).isEqualTo("hat".getBytes());
		assertThat(direct.getElement(1)).isEqualTo("it".getBytes());
		assertThat(direct.getElement(2)).isEqualTo("is".getBytes());
		assertThat(direct.getElement(3)).isEqualTo("a".getBytes());
		assertThat(direct.getId("is".getBytes())).isEqualTo(2);
		assertThat(direct.getId("ha".getBytes())).isEqualTo(4);
		assertThat(direct.getId("h".getBytes())).isEqualTo(-1);
	}

	@Test
	public void serializationTest()
			throws JsonParseException, JsonMappingException, JsonProcessingException, IOException, JSONException {

		SuccinctTrie dict = new SuccinctTrie(new DatasetId("dataset"), "name");
		dict.setDataset(new DatasetId("test"));
		dict.setName("testDict");
		data().forEach(value -> dict.put(value.getBytes()));

		dict.compress();
		SerializationTestUtil
				.forType(Dictionary.class)
				.test(dict);
	}

	@ParameterizedTest(name = "seed: {0}")
	@MethodSource("getSeeds")
	public void valid(long seed) {
		final SuccinctTrie dict = new SuccinctTrie(new DatasetId("dataset"), "name");
		EncodedDictionary direct = new EncodedDictionary(dict, StringTypeEncoded.Encoding.UTF8);
		final BiMap<String, Integer> reference = HashBiMap.create();

		AtomicInteger count = new AtomicInteger(0);

		Random random = new Random(seed);

		IntStream
				.range(0, 8192)
				.boxed()
				.sorted(TernaryTreeTestUtil.shuffle(random))
				.forEach(rep -> {
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