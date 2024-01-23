package com.bakdata.conquery.util.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class QuickSearchTest {

	@Test
	public void anaconda() {
		final TrieSearch<String> search = new TrieSearch<>(2, "");
		for (String item : List.of("Anaconda", "Honda", "London", "Analysis", "Canada", "Condor")) {
			search.addItem(item, List.of(item));
		}

		search.shrinkToFit();

		final List<String> results = search.findItems(List.of("anaconda"), Integer.MAX_VALUE);

		assertThat(results).isEqualTo(List.of("Anaconda", "Canada", "Condor", "London", "Analysis", "Honda"));

	}

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
		search.shrinkToFit();
		return search;
	}


	@Test
	public void searchOrder() {

		final TrieSearch<String> search = setup();

		// The more hits an item has, the more do we favor it.

		assertThat(search.findItems(List.of("aa", "c"), Integer.MAX_VALUE))
				.containsExactly(
						"c aa",        // Two exact matches
						"c",        // One exact match
						"aa",        // One exact match
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
	public void testNGrams() {
		final TrieSearch<String> search = new TrieSearch<>(2, null);

		assertThat(search.ngramSplitToStringStream("baaacd"))
				.containsExactly(
						"baaacd!",
						"ba",
						"aa",
						"aa",
						"ac",
						"cd"
				);

		assertThat(search.ngramSplitToStringStream("acd"))
				.containsExactly("acd!", "ac", "cd");

		assertThat(search.ngramSplitToStringStream("aacd"))
				.containsExactly("aacd!", "aa", "ac", "cd");
	}

	@Test
	public void testNoNGram() {
		final TrieSearch<String> search = new TrieSearch<>(Integer.MAX_VALUE, null);

		assertThat(search.ngramSplitToStringStream("baaacd"))
				.containsExactly("baaacd!");

		assertThat(search.ngramSplitToStringStream("acd"))
				.containsExactly("acd!");

		assertThat(search.ngramSplitToStringStream("aacd"))
				.containsExactly("aacd!");
	}
}