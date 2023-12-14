package com.bakdata.conquery.apiv1.query.concept.filter;

import java.io.IOException;
import java.math.BigDecimal;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdReferenceDeserializer;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.filters.GroupFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.QueryContextResolvable;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
@ToString(of = "value")
public abstract class FilterValue<VALUE> {
	/**
	 * Very large SELECT FilterValues can cause issues, so we just limit it to large but not gigantic quantities.
	 */
	private static final int MAX_NUMBER_FILTER_VALUES = 20_000;
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

	public SqlFilters convertToSqlFilter(ConversionContext context, SqlTables<ConceptCteStep> conceptTables) {
		FilterContext<VALUE> filterContext = new FilterContext<>(value, context, conceptTables);
		SqlFilters sqlFilters = filter.convertToSqlFilter(filterContext);
		if (context.isNegation()) {
			return new SqlFilters(sqlFilters.getSelects(), sqlFilters.getFilters().negated());
		}
		return sqlFilters;
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.MULTI_SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQMultiSelectFilter extends FilterValue<String[]> {
		public CQMultiSelectFilter(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}

		@ValidationMethod(message = "Too many values selected.")
		@JsonIgnore
		public boolean isSaneAmountOfFilterValues() {
			return getValue().length < MAX_NUMBER_FILTER_VALUES;
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.BIG_MULTI_SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQBigMultiSelectFilter extends FilterValue<String[]> {
		public CQBigMultiSelectFilter(@NsIdRef Filter<String[]> filter, String[] value) {
			super(filter, value);
		}

		@ValidationMethod(message = "Too many values selected.")
		@JsonIgnore
		public boolean isSaneAmountOfFilterValues() {
			return getValue().length < MAX_NUMBER_FILTER_VALUES;
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQSelectFilter extends FilterValue<String> {
		public CQSelectFilter(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.STRING, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQStringFilter extends FilterValue<String> {
		public CQStringFilter(@NsIdRef Filter<String> filter, String value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.INTEGER, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQIntegerFilter extends FilterValue<Long> {
		public CQIntegerFilter(@NsIdRef Filter<Long> filter, Long value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.INTEGER_RANGE, base = FilterValue.class)
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
	@CPSType(id = FrontendFilterType.Fields.MONEY_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQMoneyRangeFilter extends FilterValue<LongRange> {
		public CQMoneyRangeFilter(@NsIdRef Filter<LongRange> filter, LongRange value) {
			super(filter, value);
		}
	}


	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.REAL, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQRealFilter extends FilterValue<BigDecimal> {
		public CQRealFilter(@NsIdRef Filter<BigDecimal> filter, BigDecimal value) {
			super(filter, value);
		}
	}

	@NoArgsConstructor
	@CPSType(id = FrontendFilterType.Fields.REAL_RANGE, base = FilterValue.class)
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
	@CPSType(id = FrontendFilterType.Fields.GROUP, base = FilterValue.class)
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
	 * The resolved filter instructs the frontend on how to render and serialize the filter value using the {@link Filter#createFrontendConfig(com.bakdata.conquery.models.config.ConqueryConfig)} method. The filter must implement {@link GroupFilter} and provide the type information of the value to correctly deserialize the received object.
	 */
	public static class GroupFilterDeserializer extends StdDeserializer<GroupFilterValue> {
		private final NsIdReferenceDeserializer<FilterId, Filter<?>> nsIdDeserializer = new NsIdReferenceDeserializer<>(Filter.class, null, FilterId.class);

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
