package com.bakdata.conquery.models.identifiable.mapping.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.models.identifiable.mapping.DefaultIdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;

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
				@Override
		public IdMappingAccessor[] getIdAccessors() {
			return new IdMappingAccessor[] {
				new DefaultIdMappingAccessor(this, new int[] { 0 }),
				new DefaultIdMappingAccessor(this, new int[] { 1, 2 })
			};
		}

		@Override
		public String[] getPrintIdFields() {
			return new String[]{"first", "second", "third"};
		}

		@Override
		public String[] getHeader() {
			return new String[]{"first", "second", "third"};
		}

	}

}
