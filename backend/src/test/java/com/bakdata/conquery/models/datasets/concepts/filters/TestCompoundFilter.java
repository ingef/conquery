package com.bakdata.conquery.models.datasets.concepts.filters;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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


	public static class FEFilterProcessor {
		public static <V extends CompoundValueContainer> Map<String, FEFilter> generateFrom(Filter<V> filter, Class<V> valueClass)
				throws ConceptConfigurationException {
			final HashMap<String, FEFilter> map = new HashMap<>();

			final Map<String, Method>
					valueConfigurations =
					Arrays.stream(filter.getClass().getMethods())
						  .filter(m -> m.isAnnotationPresent(CompoundValueConfig.class))
						  .collect(Collectors.toMap(m -> m.getAnnotation(CompoundValueConfig.class).value(), Function.identity()));

			for (Field field : valueClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(CompoundValue.class)) {
					final Method method = valueConfigurations.get(field.getAnnotation(CompoundValue.class).value());
					if (method == null) {
						throw new ConceptConfigurationException(filter, "Unable to find frontend filter configuration for: " + field.getName());
					}
					try {

						final FEFilter config = (FEFilter) method.invoke(filter);
						map.put(field.getName(), config);
					}
					catch (InvocationTargetException | IllegalAccessException e) {
						throw new ConceptConfigurationException(filter, "Unable to create frontend filter configuration for: " + field.getName(), e);
					}

				}
			}
			return map;
		}
	}
}
