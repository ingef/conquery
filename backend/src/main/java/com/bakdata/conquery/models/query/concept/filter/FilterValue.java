package com.bakdata.conquery.models.query.concept.filter;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.IdReference;
import com.bakdata.conquery.models.common.Range.DoubleRange;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(of = "value")
public abstract class FilterValue<VALUE> {
	@Valid
	@NotNull
	@Nonnull
	@IdReference
	private Filter<?> filter;
	@Valid
	@NotNull
	@Nonnull
	private VALUE value;


	@NoArgsConstructor
	@CPSType(id = "MULTI_SELECT", base = FilterValue.class)
	@CPSType(id = "BIG_MULTI_SELECT", base = FilterValue.class)
	public static class CQMultiSelectFilter extends FilterValue<String[]> {
		public CQMultiSelectFilter(@IdReference Filter<?> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "SELECT", base = FilterValue.class)
	public static class CQSelectFilter extends FilterValue<String> {
		public CQSelectFilter(@IdReference Filter<?> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "STRING", base = FilterValue.class)
	public static class CQStringFilter extends FilterValue<String> {
		public CQStringFilter(@IdReference Filter<?> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "INTEGER_RANGE", base = FilterValue.class)
	public static class CQIntegerRangeFilter extends FilterValue<LongRange> {
		public CQIntegerRangeFilter(@IdReference Filter<?> filter, LongRange value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "REAL_RANGE", base = FilterValue.class)
	public static class CQRealRangeFilter extends FilterValue<DoubleRange> {
		public CQRealRangeFilter(@IdReference Filter<?> filter, DoubleRange value) {
			super(filter, value);
		}
	}
}