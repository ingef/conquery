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
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.BigMultiSelectFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.FilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.IntegerRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MoneyRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.MultiSelectFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.RealRangeFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.SelectFilterValue;
import com.bakdata.conquery.quarkus.concepts.filters.values.definitions.StringFilterValue;
import org.junit.jupiter.api.Test;

class FilterDefinitionProviderValueTypesTest {

	private static final Set<Class<? extends FilterValue>> INTEGER_RANGE = Set.of(IntegerRangeFilterValue.class);
	private static final Set<Class<? extends FilterValue>> NUMERIC_RANGES = Set.of(
			IntegerRangeFilterValue.class,
			MoneyRangeFilterValue.class,
			RealRangeFilterValue.class
	);

	@Test
	void declaresAcceptedFilterValueTypesForEveryBuiltInProvider() {
		Map<FilterDefinitionProvider<?>, Set<Class<? extends FilterValue>>> expected = Map.ofEntries(
				Map.entry(new BigMultiSelectFilterProvider(), Set.of(BigMultiSelectFilterValue.class)),
				Map.entry(new CategoryMaxSumFilterProvider(), NUMERIC_RANGES),
				Map.entry(new CountFilterProvider(), INTEGER_RANGE),
				Map.entry(new CountQuartersFilterProvider(), INTEGER_RANGE),
				Map.entry(new DateDistanceFilterProvider(), INTEGER_RANGE),
				Map.entry(new DurationSumFilterProvider(), INTEGER_RANGE),
				Map.entry(new FlagsFilterProvider(), Set.of(MultiSelectFilterValue.class)),
				Map.entry(new NumberFilterProvider(), NUMERIC_RANGES),
				Map.entry(new PrefixTextFilterProvider(), Set.of(StringFilterValue.class)),
				Map.entry(new QuartersInYearFilterProvider(), INTEGER_RANGE),
				Map.entry(new SelectFilterProvider(), Set.of(MultiSelectFilterValue.class, BigMultiSelectFilterValue.class)),
				Map.entry(new SingleSelectFilterProvider(), Set.of(SelectFilterValue.class)),
				Map.entry(new SumFilterProvider(), NUMERIC_RANGES)
		);

		expected.forEach((provider, valueTypes) -> assertEquals(valueTypes, provider.acceptedValueTypes(), provider.type()));
	}
}
