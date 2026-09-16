package com.bakdata.conquery.quarkus.plugin.api.filters;

import java.util.List;

/**
 * Backend-independent filter description emitted by a plugin provider.
 *
 * <p>Required columns are local names. The host validates them again and creates its own IDs and repository records.</p>
 */
public record FilterResult(
		String name,
		String label,
		String valueType,
		String unit,
		String tooltip,
		List<Option> options,
		Integer min,
		Integer max,
		String pattern,
		boolean allowDropFile,
		boolean creatable,
		Object defaultValue,
		List<String> requiredColumns
) {
	public FilterResult {
		options = options == null ? List.of() : List.copyOf(options);
		requiredColumns = requiredColumns == null ? List.of() : List.copyOf(requiredColumns);
	}

	public record Option(String value, String label, String optionValue) {
	}
}
