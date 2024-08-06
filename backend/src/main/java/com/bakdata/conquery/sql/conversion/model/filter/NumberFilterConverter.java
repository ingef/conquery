package com.bakdata.conquery.sql.conversion.model.filter;

import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class NumberFilterConverter<RANGE extends IRange<? extends Number, ?>> implements FilterConverter<NumberFilter<RANGE>, RANGE> {

	@Override
	public SqlFilters convertToSqlFilter(NumberFilter<RANGE> filter, FilterContext<RANGE> filterContext) {

		Column column = filter.getColumn();
		ConnectorSqlTables tables = filterContext.getTables();

		Class<? extends Number> numberClass = NumberMapUtil.getType(column);
		ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), numberClass);

		Field<? extends Number> eventFilterCtePredecessor = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.EVENT_FILTER)).select();
		IRange<? extends Number, ?> filterValue = prepareFilterValue(column, filterContext.getValue());
		NumberCondition condition = new NumberCondition(eventFilterCtePredecessor, filterValue);

		ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
														 .preprocessingSelects(List.of(rootSelect))
														 .build();

		WhereClauses whereClauses = WhereClauses.builder()
												.eventFilter(condition)
												.build();

		return new SqlFilters(selects, whereClauses);
	}

	@Override
	public Condition convertForTableExport(NumberFilter<RANGE> filter, FilterContext<RANGE> filterContext) {
		Column column = filter.getColumn();
		String tableName = column.getTable().getName();
		String columnName = column.getName();
		Field<Number> field = DSL.field(DSL.name(tableName, columnName), Number.class);
		return new NumberCondition(field, filterContext.getValue()).condition();
	}

	/**
	 * If there is a long range filter on a column of type MONEY, the filter value will represent a decimal with the point moved right 2 places right.
	 * <p>
	 * For example, the filter value {@code {min: 1000€, max: 2000€}} will be converted to {@code {min: 10,00€, max: 20,00€}}
	 */
	private static IRange<? extends Number, ?> prepareFilterValue(Column column, IRange<? extends Number, ?> filterValue) {
		if (column.getType() != MajorTypeId.MONEY || !(filterValue instanceof Range.LongRange)) {
			return filterValue;
		}
		Long min = (Long) filterValue.getMin();
		Long max = (Long) filterValue.getMax();
		return Range.LongRange.of(
				BigDecimal.valueOf(min).movePointLeft(2),
				BigDecimal.valueOf(max).movePointLeft(2)
		);
	}

}
