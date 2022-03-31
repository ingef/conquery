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
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.dictionary.Encoding;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
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

		SuccinctTrie direct = new SuccinctTrie(Dataset.PLACEHOLDER, "name", Encoding.UTF8);


		int distinctValues = 0;
		for (String entry : words) {
			int id = direct.put(entry);
			if (id > distinctValues) {
				distinctValues++;
			}
		}

		direct.compress();

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
			throws IOException, JSONException {

		final CentralRegistry registry = new CentralRegistry();
		registry.register(Dataset.PLACEHOLDER);

		SuccinctTrie dict = new SuccinctTrie(Dataset.PLACEHOLDER, "testDict", Encoding.UTF8);

		data().forEach(value -> dict.put(value));

		dict.compress();
		SerializationTestUtil
				.forType(Dictionary.class)
				.registry(registry)
				.test(dict);
	}

	@ParameterizedTest(name = "seed: {0}")
	@MethodSource("getSeeds")
	public void valid(long seed) {
		final SuccinctTrie dict = new SuccinctTrie(Dataset.PLACEHOLDER, "name", Encoding.UTF8);
		EncodedDictionary direct = new EncodedDictionary(dict);
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
					dict.add(prefix);
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
					.isEqualTo(entry.getValue());
		});
		log.info("reverse lookup done");
	}
}