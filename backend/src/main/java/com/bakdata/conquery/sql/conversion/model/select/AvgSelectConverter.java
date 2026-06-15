package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AvgFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.AvgSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import com.bakdata.conquery.sql.conversion.model.filter.*;

import java.math.BigDecimal;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * Next steps:
 * - See the new test case in src/test/resources/tests/aggregator/AVG_AGGREGATOR/filter/AVG_FILTER.test.json. Implement
 * a conversion for the {@link AvgFilter}. Therefore, turn this {@link SelectConverter} into a {@link SqlAggregator}
 * and implement {@link FilterConverter}.
 * <p>
 * Hints:
 * - The filter is applied onto the aggregated avg value. Have a look at the select converter test case you already
 * implemented, and at the filter value of the new test case. Every row, whose avg value is not in the applied
 * filter range will be filtered.
 * - Implementation is similar to COUNT/SUM filter conversion.
 * - You do not need to implement FilterConverter::convertForTableExport(). Throw an {@link UnsupportedOperationException}.
 * <p>
 * Bonus:
 * - After finishing the implementation, check if we actually need individual {@link RangeCondition} classes when
 * converting filters or can use one generic class for this purpose. In the future, I'd like to reduce complexity in
 * this repository, and getting rid of superfluous classes would be a first step.
 */
public class AvgSelectConverter<RANGE extends IRange<? extends Number, ?>> implements
// Move to a different package?
//        SqlAggregator,
        SelectConverter<AvgSelect>,
        FilterConverter<AvgFilter<RANGE>, RANGE> {

    @Override
    public ConnectorSqlSelects connectorSelect(final AvgSelect select, final SelectContext<ConnectorSqlTables> selectContext) {

        String alias = selectContext.getNameGenerator().selectName(select);
        ConnectorSqlTables tables = selectContext.getTables();
        Column avgColumn = select.getColumn().resolve();

        Class<? extends Number> numberClass = NumberMapUtil.getType(avgColumn);

        ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), avgColumn.getName(), numberClass);

        String eventFilterCte = tables.cteName(ConceptCteStep.EVENT_FILTER);

        Field<? extends Number> avgField = rootSelect
                .qualify(eventFilterCte) // change column from root table to event_filter
                .select();

        FieldWrapper<BigDecimal> avgGroupBy = new FieldWrapper<>(DSL.avg(avgField).as(alias), avgColumn.getName());

        String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
        ExtractingSqlSelect<BigDecimal> finalSelect = avgGroupBy.qualify(finalPredecessor);

        return  ConnectorSqlSelects.builder()
                .preprocessingSelect(rootSelect)
                .aggregationSelect(avgGroupBy)
                .finalSelect(finalSelect)
                .build();
    }

// TODO: Use CommonAggregationSelect?
    @Override
    public SqlFilters convertToSqlFilter(AvgFilter<RANGE> filter, FilterContext<RANGE> filterContext) {

        Column column = filter.getColumn().resolve();
        String alias = filterContext.getNameGenerator().selectName(filter); // always selectname?
        ConnectorSqlTables tables = filterContext.getTables();

        Class<? extends Number> numberClass = NumberMapUtil.getType(column);

        ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), numberClass);


        String eventFilterCte = tables.cteName(ConceptCteStep.EVENT_FILTER);
        Field<? extends Number> avgField = rootSelect.qualify(eventFilterCte).select();

        // TODO: Use the correct type
        FieldWrapper<?> avgGroupBy = new FieldWrapper<>(DSL.avg(avgField).as(alias), column.getName());


       ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
               .preprocessingSelect(rootSelect)
               .aggregationSelect(avgGroupBy)
               .build();



        String aggSelectPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
        // TODO: Use the correct type
        Field<?> qualifiedAvgSelect = avgGroupBy.qualify(aggSelectPredecessor).select();

        // TODO: Replace CountCondition
        CountCondition condition = new CountCondition(qualifiedAvgSelect, filterContext.getValue());

        WhereClauses whereClauses = WhereClauses.builder()
                .groupFilter(condition)
                .build();



        return new SqlFilters(
                selects,
                whereClauses
        );
    }

    @Override
    public Condition convertForTableExport(AvgFilter<RANGE> filter, FilterContext<RANGE> filterContext) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
