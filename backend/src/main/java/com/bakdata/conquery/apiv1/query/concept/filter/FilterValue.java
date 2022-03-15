package com.bakdata.conquery.apiv1.query.concept.filter;

import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.GroupedValueContainer;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonCreator;
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
	@NotNull
	@Nonnull
	@NsIdRef
	private Filter<VALUE> filter;

	@NotNull
	@Nonnull
	private VALUE value;


	public void resolve(QueryResolveContext context) {};

	public FilterNode<?> createNode() {
		return getFilter().createFilterNode(getValue());
	}


	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.MULTI_SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQMultiSelectFilter extends FilterValue<String[]> {
		public CQMultiSelectFilter(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.BIG_MULTI_SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQBigMultiSelectFilter extends FilterValue<String[]> {
		public CQBigMultiSelectFilter(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQSelectFilter extends FilterValue<String> {
		public CQSelectFilter(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.STRING, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQStringFilter extends FilterValue<String> {
		public CQStringFilter(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.INTEGER, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQIntegerFilter extends FilterValue<Integer> {
		public CQIntegerFilter(@NsIdRef Filter<Integer> filter, Integer value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.INTEGER_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQIntegerRangeFilter extends FilterValue<LongRange> {
		public CQIntegerRangeFilter(@NsIdRef Filter<LongRange> filter, LongRange value) {
			super(filter, value);
		}
	}

	/**
	 * @implNote Is basically the same as INTEGER_RANGE, but when a deserialized MONEY_RANGE was serialized again
	 * it became an INTEGER_RANGE, which is handled differently by the frontend.
	 */
	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.MONEY_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQMoneyRangeFilter extends FilterValue<LongRange> {
		public CQMoneyRangeFilter(@NsIdRef Filter<LongRange> filter, LongRange value) {	super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.REAL_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQRealRangeFilter extends FilterValue<Range<BigDecimal>> {
		public CQRealRangeFilter(@NsIdRef Filter<Range<BigDecimal>> filter, Range<BigDecimal> value) {
			super(filter, value);
		}
	}

	@CPSType(id = FEFilterType.Fields.GROUP, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class GroupFilter extends FilterValue<GroupedValueContainer> {
		@JsonCreator
		public GroupFilter(@NsIdRef Filter<GroupedValueContainer> filter, GroupedValueContainer value) {
			super(filter, value);
		}

		@Override
		public void resolve(QueryResolveContext context) {
			getValue().resolve(context);
		}
	}
}