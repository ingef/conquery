package com.bakdata.conquery.apiv1.query.concept.filter;

import java.math.BigDecimal;
import java.util.Set;
import javax.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.common.Range.MoneyRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.TestOnly;
import org.jooq.Condition;

/**
 * @implNote The {@link JsonCreator} annos are necessary. Otherwise, Jackson will deserilaize all values as generic objects.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@Data
@AllArgsConstructor
public abstract class FilterValue<VALUE> {

	@NotNull
	@Nonnull
	@ToString.Exclude
	private FilterId filter;

	@NotNull
	@Nonnull
	private Object value;

	public void resolve(QueryResolveContext context) {
	}

	public FilterNode<?> createNode() {
		final Filter<VALUE> resolve = (Filter<VALUE>) getFilter().resolve();
		return resolve.createFilterNode(readValue());
	}

	public VALUE readValue() {
		return ((VALUE) value);
	}

	public SqlFilters convertToSqlFilter(SqlIdColumns ids, ConversionContext context, ConnectorSqlTables tables) {
		FilterContext<VALUE> filterContext = FilterContext.forConceptConversion(ids, readValue(), context, tables);
		final Filter<VALUE> resolve = (Filter<VALUE>) filter.resolve();
		SqlFilters sqlFilters = resolve.createConverter().convertToSqlFilter(resolve, filterContext);
		return sqlFilters;
	}

	public Condition convertForTableExport(SqlIdColumns ids, ConversionContext context) {
		FilterContext<VALUE> filterContext = FilterContext.forTableExport(ids, readValue(), context);
		final Filter<VALUE> resolve = (Filter<VALUE>) filter.resolve();
		return resolve.createConverter().convertForTableExport(resolve, filterContext);
	}

	@CPSType(id = FrontendFilterType.Fields.MULTI_SELECT, base = FilterValue.class)
	public static class CQMultiSelectFilter extends FilterValue<Set<String>> {
		@JsonCreator
		public CQMultiSelectFilter(FilterId filter, Set<String> value) {
			super(filter, value);
		}

		@Override
		public String toString() {
			final String valueString;
			final int size = readValue().size();

			if (size > 20) {
				valueString = size + " values";
			}
			else {
				valueString = readValue().toString();
			}

			return "%s(value=%s)".formatted(FrontendFilterType.Fields.MULTI_SELECT, valueString);
		}

	}

	@CPSType(id = FrontendFilterType.Fields.BIG_MULTI_SELECT, base = FilterValue.class)
	public static class CQBigMultiSelectFilter extends FilterValue<Set<String>> {

		@JsonCreator
		public CQBigMultiSelectFilter(FilterId filter, Set<String> value) {
			super(filter, value);
		}

		@Override
		public String toString() {
			final String valueString;
			final int size = readValue().size();

			if (size > 20) {
				valueString = size + " values";
			}
			else {
				valueString = readValue().toString();
			}

			return "%s(value=%s)".formatted(FrontendFilterType.Fields.BIG_MULTI_SELECT, valueString);
		}
	}

	@CPSType(id = FrontendFilterType.Fields.SELECT, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQSelectFilter extends FilterValue<String> {
		@JsonCreator
		public CQSelectFilter(FilterId filter, String value) {
			super(filter, value);
		}
	}

	@CPSType(id = FrontendFilterType.Fields.STRING, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQStringFilter extends FilterValue<String> {
		@JsonCreator
		public CQStringFilter(FilterId filter, String value) {
			super(filter, value);
		}
	}

	@CPSType(id = FrontendFilterType.Fields.INTEGER, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQIntegerFilter extends FilterValue<Long> {
		@JsonCreator
		public CQIntegerFilter(FilterId filter, Long value) {
			super(filter, value);
		}
	}

	@CPSType(id = FrontendFilterType.Fields.INTEGER_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQIntegerRangeFilter extends FilterValue<LongRange> {
		@JsonCreator
		public CQIntegerRangeFilter(FilterId filter, LongRange value) {
			super(filter, value);
		}
	}

	/**
	 * @implNote Is basically the same as INTEGER_RANGE, but when a deserialized MONEY_RANGE was serialized again
	 * it became an INTEGER_RANGE, which is handled differently by the frontend.
	 */
	@CPSType(id = FrontendFilterType.Fields.MONEY_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQMoneyRangeFilter extends FilterValue<MoneyRange> {

		@JsonIgnore
		@JacksonInject(useInput = OptBoolean.FALSE)
		@NotNull
		@EqualsAndHashCode.Exclude
		@Setter(onMethod_ = {@TestOnly})
		private ConqueryConfig config;

		@JsonCreator
		public CQMoneyRangeFilter(FilterId filter, LongRange value) {
			super(filter, value);
		}

		@Override
		public MoneyRange readValue() {
			return MoneyRange.fromNumberRange((LongRange) getValue(), config.getFrontend().getCurrency());
		}
	}


	@CPSType(id = FrontendFilterType.Fields.REAL, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQRealFilter extends FilterValue<Double> {
		@JsonCreator
		public CQRealFilter(FilterId filter, BigDecimal value) {
			super(filter, value);
		}
	}

	@CPSType(id = FrontendFilterType.Fields.REAL_RANGE, base = FilterValue.class)
	@ToString(callSuper = true)
	public static class CQRealRangeFilter extends FilterValue<Range<BigDecimal>> {
		@JsonCreator
		public CQRealRangeFilter(FilterId filter, Range<BigDecimal> value) {
			super(filter, value);
		}

	}
}
