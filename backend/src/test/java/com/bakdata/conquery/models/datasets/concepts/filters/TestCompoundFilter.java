package com.bakdata.conquery.models.datasets.concepts.filters;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CompoundValueConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.CompoundValueContainer;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.filter.event.MultiSelectFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@CPSType(id = "TEST_COMPOUND_FILTER", base = Filter.class)
public class TestCompoundFilter extends SingleColumnFilter<TestCompoundFilter.CompoundFilterValue> {

	private final static String STRINGS = "string";
	private final static String REPETITIONS = "repetitions";

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(FEFilterType.Fields.GROUP);
		f.setFilters(FEFilterProcessor.generateFrom(this, TestCompoundFilter.CompoundFilterValue.class));
		f.setGroupType(TestCompoundFilter.CompoundFilterValue.class.getAnnotation(CPSType.class).id());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getColumn()};
	}

	@Override
	public FilterNode<?> createFilterNode(TestCompoundFilter.CompoundFilterValue compoundFilterValue) {
		return new MultiSelectFilterNode(getColumn(), compoundFilterValue.getResolvedValues());
	}

	@CompoundValueConfig(STRINGS)
	public FEFilter zipCodeConfig() {
		return FEFilter.builder()
					   .label("Elements")
					   .type(FEFilterType.Fields.MULTI_SELECT)
					   .build();
	}

	@CompoundValueConfig(REPETITIONS)
	public FEFilter radiusConfig() {
		return FEFilter.builder()
					   .label("Maximum Repetitions")
					   .type(FEFilterType.Fields.INTEGER_RANGE)
					   .build();
	}


	@CPSType(id = "TEST_COMPOUND_VALUE", base = CompoundValueContainer.class)
	@Getter
	@AllArgsConstructor
	public static class CompoundFilterValue implements CompoundValueContainer {
		@CompoundValue(STRINGS)
		private String[] strings;

		@CompoundValue(REPETITIONS)
		private int repetitions;

		@InternalOnly
		private String[] resolvedValues;

		@Override
		public void resolve(QueryResolveContext context) {
			ArrayList<String> values = new ArrayList<>();
			for (String string : strings) {
				IntStream.range(1, repetitions + 1).mapToObj(string::repeat).sequential().collect(Collectors.toCollection(() -> values));
			}
			resolvedValues = values.toArray(String[]::new);
		}
	}


}
