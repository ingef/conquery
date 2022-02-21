package com.bakdata.conquery.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.trie.PatriciaTrie;

@NoArgsConstructor
public class TrieSearch<T> {
	private static final Pattern SPLIT = Pattern.compile("[\\s()_,:-]+");
	private final PatriciaTrie<List<T>> trie = new PatriciaTrie<>();

	private Stream<String> split(String keyword) {
		return SPLIT.splitAsStream(keyword.trim())
					.map(String::trim)
					.filter(Predicate.not(Strings::isNullOrEmpty))
					.map(String::toLowerCase);
	}

	public List<T> findItems(Collection<String> keywords, int limit) {
		return keywords.stream()
					   .flatMap(this::split)
					   .map(trie::prefixMap)
					   .map(Map::values)
					   .flatMap(Collection::stream)
					   .flatMap(List::stream)
					   //TODO sorting would probably be neat (You can probably just sort by length ascending)
					   .limit(limit)
					   .collect(Collectors.toList());
	}

	public void addItem(T item, List<String> keywords) {
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.forEach(kw -> trie.computeIfAbsent(kw, (ignored) -> new ArrayList<>()).add(item));
	}

	public Collection<T> listItems() {
		return trie.values().stream().flatMap(List::stream).collect(Collectors.toList());
	}

	public long calculateSize() {
		return trie.values().stream().distinct().count();
	}

}
