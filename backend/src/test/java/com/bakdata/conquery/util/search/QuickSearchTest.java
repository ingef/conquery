package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class QuickSearchTest {


	private final TrieSearch<String> quickSearch = new TrieSearch<>();

	private void fill(TrieSearch<String> search, List<String> items) {
		for (String item : items) {
			search.addItem(item, List.of(item));
		}
	}

	@BeforeEach
	public void setup() {
		quickSearch.clear();

		List<String> items = List.of(
				"a",
				"aa",
				"aaa",
				"aab",
				"b",
				"c",
				"c aa",
				"d baaacd"
		);

		fill(quickSearch, items);
	}


	@Test
	public void searchOrder() {

		// The more hits an item has, the more do we favor it.

		assertThat(quickSearch.findItems(List.of("aa", "c"), Integer.MAX_VALUE))
				.containsExactly(
						"c aa",		// Two exact matches
						"aa",		// One exact match
						"c",		// One exact match
						"aaa",		// One prefix match, onto a whole word
						"aab",		// One prefix match, onto a whole word
						"d baaacd"	// Two partial matches
				);

		// However negative matches are not considered (ie "c" is not used to weigh against "c aa")
		assertThat(quickSearch.findItems(List.of("aa"), 4)).containsExactly("aa", "c aa", "aaa", "aab");
	}

	@Test
	public void searchIdentities() {

		// Exact matches should be first
		assertThat(quickSearch.findItems(List.of("a"), 1)).containsExactly("a");
		assertThat(quickSearch.findItems(List.of("aa"), 1)).containsExactly("aa");
		assertThat(quickSearch.findItems(List.of("acd"), 1)).containsExactly("d baaacd");

	}

	@Test
	public void testSuffixes() {
		assertThat(TrieSearch.suffixes("baaacd"))
				.containsExactly(
						"baaacd!",
						"aaacd",
						"aacd",
						"acd"
				);

		assertThat(TrieSearch.suffixes("acd"))
				.containsExactly("acd!");

		assertThat(TrieSearch.suffixes("aacd"))
				.containsExactly("aacd!", "acd");
	}
}