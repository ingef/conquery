package com.bakdata.conquery.models.identifiable.mapping.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;

import static org.assertj.core.api.Assertions.assertThat;


public class IdMappingTest {

	public static Stream<Arguments> testData() {
		List<Pair<String, List<String>>> data = new ArrayList<>();
		data.add(Pair.of("123", Arrays.asList("a", "d", "h")));
		data.add(Pair.of("124", Arrays.asList("b", "e", "i")));
		data.add(Pair.of("125", Arrays.asList("c", "f", "j")));
		data.add(Pair.of("126", Arrays.asList("d", "g", "k")));

		return Stream.of(Arguments.of(data));
	}

	@ParameterizedTest @MethodSource({ "testData" })
	public void generalTest(List<Pair<String, List<String>>> data) {
		TestIdMappingConfig mappingConfig = new TestIdMappingConfig();
	}

	public class TestIdMappingConfig extends IdMappingConfig {
		public TestIdMappingConfig(){
			super();
		}

		@Override public List<IdMappingAccessor> getIdAccessors() {
			ArrayList<IdMappingAccessor> idAccessors = new ArrayList<>();
			idAccessors.add(new IdMappingAccessor(this, Arrays.asList(0)));
			idAccessors.add(new IdMappingAccessor(this, Arrays.asList(1, 2)));
			return idAccessors;
		}

		@Override public List<String> getPrintIdFields() {
			return Arrays.asList("first", "second", "third");
		}

		@Override public List<String> getHeader() {
			return Arrays.asList("first", "second", "third");
		}

	}

}
