package com.bakdata.conquery.resources.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.util.search.internal.Cursor;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class ConceptsProcessorTest {

	@Test
	public void testCursor() {
		final Cursor<Integer> cursor = new Cursor<>(IntStream.rangeClosed(0, 10).boxed().iterator());

		assertThat(cursor.get(0, 1)).containsExactly(0, 1);
		assertThat(cursor.get(1, 1)).containsExactly(1);

		assertThat(cursor.get(1, 3)).containsExactly(1, 2, 3);

		assertThat(cursor.get(0, 10)).isEqualTo(IntStream.rangeClosed(0, 10).boxed().collect(Collectors.toList()));

		assertThat(cursor.get(0, Integer.MAX_VALUE)).isEqualTo(IntStream.rangeClosed(0, 10).boxed().collect(Collectors.toList()));

	}

	@Test
	public void mapInsertion() {
		Object2LongMap<FrontendValue> map = new Object2LongOpenHashMap<>();

		map.put(new FrontendValue("a", "label1"), 1);
		map.put(new FrontendValue("a", "label2"), 1);

		// canary for changes of EqualsAndHashcode behaviour
		assertThat(map).hasSize(1);
	}

}