package com.bakdata.conquery.models.query.concept.filter;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
	@NsIdRef
	private Filter<?> filter;
	@Valid
	@NotNull
	@Nonnull
	private VALUE value;


	@NoArgsConstructor
	@CPSType(id = "MULTI_SELECT", base = FilterValue.class)
	@CPSType(id = "BIG_MULTI_SELECT", base = FilterValue.class)
	public static class CQMultiSelectFilter extends FilterValue<String[]> {
		public CQMultiSelectFilter(@NsIdRef Filter<?> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "SELECT", base = FilterValue.class)
	public static class CQSelectFilter extends FilterValue<String> {
		public CQSelectFilter(@NsIdRef Filter<?> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "STRING", base = FilterValue.class)
	public static class CQStringFilter extends FilterValue<String> {
		public CQStringFilter(@NsIdRef Filter<?> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "INTEGER_RANGE", base = FilterValue.class)
	public static class CQIntegerRangeFilter extends FilterValue<LongRange> {
		public CQIntegerRangeFilter(@NsIdRef Filter<?> filter, LongRange value) {
			super(filter, value);
		}
	}

	/**
	 * @implNote Is basically the same as INTEGER_RANGE, but when a deserialized MONEY_RANGE was serialized again
	 * it became an INTEGER_RANGE, which is handled differently by the frontend.
	 */
	@NoArgsConstructor
	@CPSType(id = "MONEY_RANGE", base = FilterValue.class)
	public static class CQMoneyRangeFilter extends FilterValue<LongRange> {
		public CQMoneyRangeFilter(@NsIdRef Filter<?> filter, LongRange value) {	super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "REAL_RANGE", base = FilterValue.class)
	public static class CQRealRangeFilter extends FilterValue<Range<BigDecimal>> {
		public CQRealRangeFilter(@NsIdRef Filter<?> filter, Range<BigDecimal> value) {
			super(filter, value);
		}
	}
}