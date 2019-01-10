package com.bakdata.conquery.models.identifiable.mapping.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bakdata.conquery.models.identifiable.mapping.IdMapping;
import com.bakdata.conquery.models.identifiable.mapping.IdMapping.IdMappingAccessor;


public class IdMappingStoreTest {
	public static Stream<Arguments> testData(){
		List<Pair<String, List<String>>> data = new ArrayList<>();
		data.add(Pair.of("123", Arrays.asList("a","d","h")));
		data.add(Pair.of("124", Arrays.asList("b","e","i")));
		data.add(Pair.of("125", Arrays.asList("c","f","j")));
		data.add(Pair.of("126", Arrays.asList("d","g","k")));

		return Stream.of(
				Arguments.of(data));
	}

	@ParameterizedTest @MethodSource({"testData"})
	public void generalTest(List<Pair<String, List<String>>> data) {
		TestIdMappingStore store = new TestIdMappingStore(data);

		for(Pair<String, List<String>> entry : data) {
			assertThat(store.toPrintId(entry.getKey())).isEqualTo(entry.getValue());
			for (IdMappingAccessor accessor: store.getIdAccessors()) {
				List<String> externalKey = Stream
						.generate(()->(String) null)
						.limit(store.getPrintIdFields().size())
						.collect(Collectors.toList());
				// set only the requested fields
				for (Integer columnOffset : accessor.getIdsUsed()) {
					externalKey.set(columnOffset, entry.getValue().get(columnOffset));
				}
				assertThat(store.toCsvId(externalKey).equals(entry.getKey()));
			}
		}
	}
	
	public class TestIdMappingStore extends IdMapping{

		public TestIdMappingStore(List<Pair<String, List<String>>> data) {
			super(data);
		}

		@Override
		public List<IdMappingAccessor> getIdAccessors() {
			ArrayList<IdMappingAccessor> idAccessors = new ArrayList<>();
			idAccessors.add(new IdMappingAccessor(Arrays.asList(0)));
			idAccessors.add(new IdMappingAccessor(Arrays.asList(1,2)));
			return idAccessors;
		}

		@Override
		public List<String> getPrintIdFields() {
			return Arrays.asList("first","second","third");
		}
		
	}

}
