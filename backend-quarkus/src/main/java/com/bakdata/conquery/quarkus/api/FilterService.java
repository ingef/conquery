package com.bakdata.conquery.quarkus.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class FilterService {

	public FilterResolveResult resolveFilterValues(String filterId, List<String> values) {
		String tableId = extractTableId(filterId);
		List<ResolvedFilterValue> resolved = values.stream()
											 .map(String::trim)
											 .filter(value -> !value.isEmpty())
											 .distinct()
											 .map(value -> new ResolvedFilterValue(value, value, value))
											 .toList();

		return new FilterResolveResult(
				new ResolvedFilter(tableId, filterId, resolved),
				List.of()
		);
	}

	public FilterAutocompleteResult autocomplete(String filterId, Optional<String> text, OptionalInt page, OptionalInt pageSize) {
		int resolvedPage = page.orElse(0);
		int resolvedPageSize = pageSize.orElse(50);
		if (resolvedPage < 0) {
			throw new BadRequestException("page must be >= 0");
		}
		if (resolvedPageSize < 1) {
			throw new BadRequestException("pageSize must be >= 1");
		}

		String prefix = text.map(String::trim).orElse("");
		List<String> candidates = List.of(prefix, prefix + "1", prefix + "2", prefix + "3")
								   .stream()
								   .map(String::trim)
								   .filter(value -> !value.isEmpty())
								   .collect(java.util.stream.Collectors.collectingAndThen(
										   java.util.stream.Collectors.toCollection(LinkedHashSet::new),
										   List::copyOf
								   ));

		int fromIndex = Math.min(resolvedPage * resolvedPageSize, candidates.size());
		int toIndex = Math.min(fromIndex + resolvedPageSize, candidates.size());
		List<AutocompleteValue> values = candidates.subList(fromIndex, toIndex)
										  .stream()
										  .map(value -> new AutocompleteValue(value, value, value, Map.of(), false))
										  .toList();

		return new FilterAutocompleteResult(values, candidates.size());
	}

	private String extractTableId(String filterId) {
		int separatorIndex = filterId.indexOf(':');
		return separatorIndex > 0 ? filterId.substring(0, separatorIndex) : filterId;
	}

	public record FilterResolveResult(
			ResolvedFilter resolvedFilter,
			List<String> unknownCodes
	) {
	}

	public record ResolvedFilter(
			String tableId,
			String filterId,
			List<ResolvedFilterValue> value
	) {
	}

	public record ResolvedFilterValue(
			String label,
			String value,
			String optionValue
	) {
	}

	public record FilterAutocompleteResult(
			List<AutocompleteValue> values,
			long total
	) {
	}

	public record AutocompleteValue(
			String label,
			String value,
			String optionValue,
			Map<String, String> templateValues,
			boolean disabled
	) {
	}
}
