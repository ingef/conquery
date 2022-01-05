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

/**
 *
 * @apiNote SUBMITTED_VALUE and FILTER_VALUE are usually the same, but can be different for {@link FilterValue}s implementing resolve.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(of = "value")
public abstract class FilterValue<SUBMITTED_VALUE, FILTER_VALUE> {
	@NotNull
	@NsIdRef
	private Filter<FILTER_VALUE> filter;

	/**
	 * Submitted value from query
	 */
	@NotNull
	private SUBMITTED_VALUE value;

	/**
	 * Is executed on a shard
	 * @return A query param node.
	 */
	public abstract FilterNode<?> createNode();

	public abstract void resolve(QueryResolveContext context);

	/**
	 * Simplified FilterValue which can forward is value into a filter node without transformation
	 * @param <F_VALUE>
	 */
	@NoArgsConstructor
	private abstract static class Forwarding<F_VALUE> extends FilterValue<F_VALUE, F_VALUE> {

		public Forwarding(Filter<F_VALUE> filter, F_VALUE value) {
			super(filter, value);
		}

		public FilterNode<?> createNode() {
			return getFilter().createFilterNode(getValue());
		}

		public void resolve(QueryResolveContext context){}

	}


	@NoArgsConstructor
	@CPSType(id = "MULTI_SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class MultiSelectFilterValue extends Forwarding<String[]> {
		public MultiSelectFilterValue(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "BIG_MULTI_SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class BigMultiSelectFilterValue extends Forwarding<String[]> {
		public BigMultiSelectFilterValue(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "SELECT", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class SelectFilterValue extends Forwarding<String> {
		public SelectFilterValue(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "STRING", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class StringFilterValue extends Forwarding<String> {
		public StringFilterValue(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "INTEGER_RANGE", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class IntegerRangeFilterValue extends Forwarding<LongRange> {
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
	public static class MoneyRangeFilterValue extends Forwarding<LongRange> {
		public MoneyRangeFilterValue(@NsIdRef Filter<LongRange> filter, LongRange value) {	super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = "REAL_RANGE", base = FilterValue.class)
	@ToString(callSuper = true)
	public static class RealRangeFilterValue extends Forwarding<Range<BigDecimal>> {
		public RealRangeFilterValue(@NsIdRef Filter<Range<BigDecimal>> filter, Range<BigDecimal> value) {
			super(filter, value);
		}
	}

	@CPSType(id = "POSTAL_CODE", base = FilterValue.class)
	@NoArgsConstructor
	public static class PostalCodeFilterValue extends FilterValue<PostalCodeSearchEntity, String[]> {

		@JacksonInject
		private PostalCodesManager postalCodesManager;

		/**
		 * Resolved value on Manager, that is transferred to the shard and can be feed in to the filter.
		 */
		@InternalOnly
		@Getter
		private String[] resolvedValue;

		@Override
		public FilterNode<?> createNode() {
			return getFilter().createFilterNode(resolvedValue);
		}

		@Override
		public void resolve(QueryResolveContext context) {
			Preconditions.checkNotNull(postalCodesManager);
			final PostalCodeSearchEntity value = getValue();
			
			resolvedValue = postalCodesManager.filterAllNeighbours(Integer.parseInt(value.getPlz()), value.getRadius());
		}
	}
}