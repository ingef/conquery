package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

	@Test
	public void insertAndSearch() {
		final QuickSearch<String> quickSearch = new QuickSearch<>();

		quickSearch.addItem("a b c", "a b c");

		quickSearch.addItem("abc def", "abc def");

		quickSearch.addItem("def ghiabc", "def ghiabc");

		quickSearch.listItems();
		log.info("{}", quickSearch.findItems("h", 10));
	}

	@Test
	public void trieInsertAndSearch() {
		final TrieSearch<String> quickSearch = new TrieSearch<>();

		quickSearch.addItem("a b c", List.of("a b c"));

		quickSearch.addItem("abc def", List.of("abc def"));

		quickSearch.addItem("def ghiabc", List.of("def ghiabc"));

		quickSearch.listItems();
		log.info("{}", quickSearch.findItems("d"));
	}

}