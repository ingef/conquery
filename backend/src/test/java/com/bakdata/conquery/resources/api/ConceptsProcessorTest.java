package com.bakdata.conquery.resources.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bakdata.conquery.util.search.Cursor;
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

}