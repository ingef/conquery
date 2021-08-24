package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class QuickSearchTest {
	@Test
	public void listItems() {
		final QuickSearch<Integer> quickSearch = new QuickSearch<>();

		quickSearch.addItem(0, "b");
		quickSearch.addItem(1, "c");
		quickSearch.addItem(2, "a");

		assertThat(quickSearch.listItems())
				.containsExactly(0, 1, 2);


	}

}