package com.bakdata.conquery.apiv1.query.concept.filter;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.postalcode.PostalCodeSearchEntity;
import com.bakdata.conquery.models.datasets.concepts.filters.postalcode.PostalCodesManager;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(of = "value")
public abstract class FilterValue<FILTER_VALUE> {
	@NotNull
	@NsIdRef
	private Filter<FILTER_VALUE> filter;

	/**
	 * Submitted value from query
	 */
	@NotNull
	private FILTER_VALUE value;

	/**
	 * Is executed on a shard
	 * @return A query param node.
	 */
	public FilterNode<?> createNode() {
		return getFilter().createFilterNode(getValue());
	}
	public  void resolve(QueryResolveContext context){}


	@NoArgsConstructor
	@CPSType(id = "MULTI_SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class MultiSelectFilterValue extends FilterValue<String[]> {
		public MultiSelectFilterValue(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "BIG_MULTI_SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class BigMultiSelectFilterValue extends FilterValue<String[]> {
		public BigMultiSelectFilterValue(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class SelectFilterValue extends FilterValue<String> {
		public SelectFilterValue(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "STRING", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class StringFilterValue extends FilterValue<String> {
		public StringFilterValue(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "INTEGER_RANGE", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class IntegerRangeFilterValue extends FilterValue<LongRange> {
		public IntegerRangeFilterValue(@NsIdRef Filter<LongRange> filter, LongRange value) {
			super(filter, value);
		}
	}

	/**
	 * @implNote Is basically the same as INTEGER_RANGE, but when a deserialized MONEY_RANGE was serialized again
	 * it became an INTEGER_RANGE, which is handled differently by the frontend.
	 */
	@NoArgsConstructor
	@CPSType(id = "MONEY_RANGE", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class MoneyRangeFilterValue extends FilterValue<LongRange> {
		public MoneyRangeFilterValue(@NsIdRef Filter<LongRange> filter, LongRange value) {	super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "REAL_RANGE", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class RealRangeFilterValue extends FilterValue<Range<BigDecimal>> {
		public RealRangeFilterValue(@NsIdRef Filter<Range<BigDecimal>> filter, Range<BigDecimal> value) {
			super(filter, value);
		}
	}

	@CPSType(id = "POSTAL_CODE", base = FilterValue.class)
	@NoArgsConstructor
	public static class PostalCodeFilterValue extends FilterValue<PostalCodeSearchEntity> {

		public PostalCodeFilterValue(@NsIdRef Filter<PostalCodeSearchEntity> filter, PostalCodeSearchEntity value){ super(filter,value);}
		@Override
		public void resolve(QueryResolveContext context) {
			getValue().resolve(context);
		}
	}
}