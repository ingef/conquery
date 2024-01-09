package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class QuickSearchTest {

	@Test
	public void pants() {
		final TrieSearch<String> search = new TrieSearch<>(2, "");
		for (String item : List.of("Pants", "Pantshop", "Sweatpants", "PantsPants")) {
			search.addItem(item, List.of(item));
		}

		search.shrinkToFit();

		final List<String> results = search.findItems(List.of("pants"), Integer.MAX_VALUE);

		assertThat(results).isEqualTo(List.of("Pants", "PantsPants", "Pantshop", "Sweatpants"));

	}


	private static TrieSearch<String> setup() {
		final TrieSearch<String> search = new TrieSearch<>(2, "");

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

		for (String item : items) {
			search.addItem(item, List.of(item));
		}
		return search;
	}


	@Test
	public void searchOrder() {

		final TrieSearch<String> search = setup();

		// The more hits an item has, the more do we favor it.

		assertThat(search.findItems(List.of("aa", "c"), Integer.MAX_VALUE))
				.containsExactly(
						"c aa",        // Two exact matches
						"aa",        // One exact match
						"c",        // One exact match
						"aaa",        // One prefix match, onto a whole word
						"aab",        // One prefix match, onto a whole word
						"d baaacd"    // Two partial matches
				);

		// However negative matches are not considered (ie "c" is not used to weigh against "c aa")
		assertThat(search.findItems(List.of("aa"), 4)).containsExactly("aa", "c aa", "aaa", "aab");
	}

	@Test
	public void searchIdentities() {
		final TrieSearch<String> search = setup();


		// Exact matches should be first
		assertThat(search.findItems(List.of("a"), 1)).containsExactly("a");
		assertThat(search.findItems(List.of("aa"), 1)).containsExactly("aa");
		assertThat(search.findItems(List.of("acd"), 1)).containsExactly("d baaacd");

	}

	@Test
	public void testSuffixes() {
		final TrieSearch<String> search = new TrieSearch<>(2, null);

		assertThat(search.ngramSplitStrings("baaacd", "item"))
				.containsExactly(
						"baaacd",
						"aa",
						"aa",
						"ac",
						"cd"
				);

		assertThat(search.ngramSplitStrings("acd", "item"))
				.containsExactly("acd", "cd");

		assertThat(search.ngramSplitStrings("aacd", "item"))
				.containsExactly("aacd", "ac", "cd");
	}

	@Test
	public void testNoSuffix() {
		final TrieSearch<String> search = new TrieSearch<>(Integer.MAX_VALUE, null);

		assertThat(search.ngramSplitStrings("baaacd", "item"))
				.containsExactly("baaacd");

		assertThat(search.ngramSplitStrings("acd", "item"))
				.containsExactly("acd");

		assertThat(search.ngramSplitStrings("aacd", "item"))
				.containsExactly("aacd");
	}
}