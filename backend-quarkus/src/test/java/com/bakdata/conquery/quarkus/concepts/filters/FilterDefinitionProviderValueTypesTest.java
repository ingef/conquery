package com.bakdata.conquery.quarkus.concepts.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.quarkus.concepts.filters.providers.BigMultiSelectFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.CategoryMaxSumFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.CountFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.CountQuartersFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.DateDistanceFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.DurationSumFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.FlagsFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.NumberFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.PrefixTextFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.QuartersInYearFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.SelectFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.SingleSelectFilterProvider;
import com.bakdata.conquery.quarkus.concepts.filters.providers.SumFilterProvider;
import com.bakdata.conquery.quarkus.plugin.api.filters.FilterDefinitionProvider;
import org.junit.jupiter.api.Test;

class FilterDefinitionProviderValueTypesTest {

	private static final Set<String> INTEGER_RANGE = Set.of("INTEGER_RANGE");
	private static final Set<String> NUMERIC_RANGES = Set.of("INTEGER_RANGE", "MONEY_RANGE", "REAL_RANGE");

	@Test
	void declaresAcceptedFilterValueTypesForEveryBuiltInProvider() {
		Map<FilterDefinitionProvider<?>, Set<String>> expected = Map.ofEntries(
				Map.entry(new BigMultiSelectFilterProvider(), Set.of("BIG_MULTI_SELECT")),
				Map.entry(new CategoryMaxSumFilterProvider(), NUMERIC_RANGES),
				Map.entry(new CountFilterProvider(), INTEGER_RANGE),
				Map.entry(new CountQuartersFilterProvider(), INTEGER_RANGE),
				Map.entry(new DateDistanceFilterProvider(), INTEGER_RANGE),
				Map.entry(new DurationSumFilterProvider(), INTEGER_RANGE),
				Map.entry(new FlagsFilterProvider(), Set.of("MULTI_SELECT")),
				Map.entry(new NumberFilterProvider(), NUMERIC_RANGES),
				Map.entry(new PrefixTextFilterProvider(), Set.of("STRING")),
				Map.entry(new QuartersInYearFilterProvider(), INTEGER_RANGE),
				Map.entry(new SelectFilterProvider(), Set.of("MULTI_SELECT", "BIG_MULTI_SELECT")),
				Map.entry(new SingleSelectFilterProvider(), Set.of("SELECT")),
				Map.entry(new SumFilterProvider(), NUMERIC_RANGES)
		);

		expected.forEach((provider, valueTypes) -> assertEquals(valueTypes, provider.acceptedValueTypes(), provider.type()));
	}
}
