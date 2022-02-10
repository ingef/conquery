package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.apiv1.frontend.FEFilter;
import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@CPSType(id = "TEST_COMPOUND_FILTER", base = Filter.class)
public class TestCompoundFilter extends Filter<TestCompoundFilter.CompoundFilterValue> {

	@Override
	public void configureFrontend(FEFilter f) throws ConceptConfigurationException {
		f.setType(FEFilterType.Fields.GROUP);
		f.setFilters(FEFilterProcessor.generateFrom(TestCompoundFilter.CompoundFilterValue.class));
		f.setGroupType(TestCompoundFilter.CompoundFilterValue.class.getAnnotation(CPSType.class).id());
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[0];
	}

	@Override
	public FilterNode<?> createFilterNode(CompoundFilterValue compoundFilterValue) {
		return null;
	}


	@CPSType(id = "TEST_COMPOUND_VALUE", base = FilterValue.ValueCompound.class)
	@Getter
	@AllArgsConstructor
	public static class CompoundFilterValue implements FilterValue.ValueCompound {
		@CompoundValue(FEFilterType.Fields.MULTI_SELECT)
		private String[] zipCodes;

		@CompoundValue(FEFilterType.Fields.REAL_RANGE)
		private float radius;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface CompoundValue {
		public String value();
	}

	public static class FEFilterProcessor {
		public static Map<String, FEFilter> generateFrom(Class<? extends FilterValue.ValueCompound> valueClass) {
			final HashMap<String, FEFilter> map = new HashMap<>();
			for (Field field : valueClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(CompoundValue.class)) {
					final FEFilter filter = FEFilter.builder()
													.type(field.getAnnotation(CompoundValue.class).value())
													.build();

					map.put(field.getName(), filter);

				}
			}
			return map;
		}
	}
}
