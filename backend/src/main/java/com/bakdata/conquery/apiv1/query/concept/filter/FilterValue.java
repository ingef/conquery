package com.bakdata.conquery.apiv1.query.concept.filter;

import java.io.IOException;
import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEFilterType;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdReferenceDeserializer;
import com.bakdata.conquery.io.jackson.serializer.SerdesTarget;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.QueryContextResolvable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
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
	public static class CQIntegerFilter extends FilterValue<Long> {
		public CQIntegerFilter(@NsIdRef Filter<Long> filter, Long value) {
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
		public CQMoneyRangeFilter(@NsIdRef Filter<LongRange> filter, LongRange value) {
			super(filter, value);
		}
	}


	@NoArgsConstructor
	@CPSType(id = FEFilterType.Fields.REAL, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQRealFilter extends FilterValue<BigDecimal> {
		public CQRealFilter(@NsIdRef Filter<BigDecimal> filter, BigDecimal value) {
			super(filter, value);
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

	/**
	 * A filter value that consists of multiple inputs that are grouped together into one form.
	 * <p>
	 * See TestGroupFilter in the tests for an example.
	 */
	@CPSType(id = FEFilterType.Fields.GROUP, base = FilterValue.class)
	@ToString(callSuper = true)
	@JsonDeserialize(using = GroupFilterDeserializer.class)
	public static class GroupFilterValue extends FilterValue<Object> {

		public GroupFilterValue(Filter<Object> filter, Object value) {
			super(filter, value);
		}

		@Override
		public void resolve(QueryResolveContext context) {
			if (getValue() instanceof QueryContextResolvable) {
				((QueryContextResolvable) getValue()).resolve(context);
			}
		}
	}

	/**
	 * Values of group filters can have an arbitrary format which is set by the filter itself.
	 * Hence, we treat the value for the filter as Object.class.
	 * <p>
	 * The resolved filter instructs the frontend on how to render and serialize the filter value using the {@link Filter#createFrontendConfig()} method. The filter must implement {@link GroupFilter} and provide the type information of the value to correctly deserialize the received object.
	 */
	public static class GroupFilterDeserializer extends StdDeserializer<GroupFilterValue> {
		private final NsIdReferenceDeserializer<FilterId, Filter<?>> nsIdDeserializer = new NsIdReferenceDeserializer<>(Filter.class, null, FilterId.class, SerdesTarget.MANAGER_AND_SHARD);

		protected GroupFilterDeserializer() {
			super(GroupFilterValue.class);
		}


		@Override
		@SneakyThrows
		public GroupFilterValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final TreeNode treeNode = p.getCodec().readTree(p);

			// First parse the filter id and resolve the filter
			final TreeNode filterNode = treeNode.get("filter");
			final JsonParser filterTraverse = filterNode.traverse();
			filterTraverse.nextToken();
			final Filter<?> filter = nsIdDeserializer.deserialize(filterTraverse, ctxt);

			if (!(filter instanceof GroupFilter)) {
				throw InvalidTypeIdException.from(filterNode.traverse(), GroupFilter.class, String.format("Expected filter of type %s but was: %s", GroupFilter.class, filter != null ? filter.getClass() : null));
			}
			GroupFilter groupFilter = (GroupFilter) filter;

			// Second parse the value for the filter
			final TreeNode valueNode = treeNode.get("value");
			final JsonParser valueTraverse = valueNode.traverse();
			valueTraverse.nextToken();
			final QueryContextResolvable value = ctxt.readValue(valueTraverse, groupFilter.getFilterValueType(ctxt.getTypeFactory()));

			// At last put everything into a container
			return new GroupFilterValue((Filter<Object>) filter, value);
		}

	}
}