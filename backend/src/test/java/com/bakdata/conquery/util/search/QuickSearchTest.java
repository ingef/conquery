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


	private void fill(TrieSearch<String> search, List<String> items) {
		for (String item : items) {
			search.addItem(item, List.of(item));
		}
	}

	@Test
	public void trieInsertAndSearch() {
		final TrieSearch<String> quickSearch = new TrieSearch<>();

		List<String> items = List.of(
				"a",
				"aa",
				"aaa",
				"aab",
				"b",
				"c",
				"c aa"
		);

		fill(quickSearch, items);


		// Exact matches should be first
		assertThat(quickSearch.findItems(List.of("a"), 1)).containsExactly("a");
		assertThat(quickSearch.findItems(List.of("aa"), 1)).containsExactly("aa");

		// The more hits an item has, the more do we favor it.
		assertThat(quickSearch.findItems(List.of("aa", "c"), 3)).containsExactly("c aa", "aa", "c");
		// However negative matches are not considered
		assertThat(quickSearch.findItems(List.of("aa"), 4)).containsExactly("aa", "c aa", "aaa", "aab");

	}
}