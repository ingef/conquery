package com.bakdata.conquery.sql.conversion.model.aggregator;

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

import com.bakdata.conquery.sql.conversion.model.select.*;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class AvgSqlAggregator<RANGE extends IRange<? extends Number, ?>> implements
        SqlAggregator,
        SelectConverter<AvgSelect>,
        FilterConverter<AvgFilter<RANGE>, RANGE> {

    @Override
    public ConnectorSqlSelects connectorSelect(final AvgSelect select, final SelectContext<ConnectorSqlTables> selectContext) {

        String alias = selectContext.getNameGenerator().selectName(select);
        ConnectorSqlTables tables = selectContext.getTables();
        Column column = select.getColumn().resolve();

        CommonAggregationSelect<BigDecimal> commonAggSelect = createCommonAggregationselect(column, alias, tables);

        String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
        ExtractingSqlSelect<BigDecimal> finalSelect = commonAggSelect.getGroupBy().qualify(finalPredecessor);

        return ConnectorSqlSelects.builder()
                .preprocessingSelects(commonAggSelect.getRootSelects())
                .aggregationSelect(commonAggSelect.getGroupBy())
                .finalSelect(finalSelect)
                .build();
    }

    @Override
    public SqlFilters convertToSqlFilter(AvgFilter<RANGE> filter, FilterContext<RANGE> filterContext) {

        Column column = filter.getColumn().resolve();
        String alias = filterContext.getNameGenerator().selectName(filter);
        ConnectorSqlTables tables = filterContext.getTables();

        CommonAggregationSelect<BigDecimal> commonAggSelect = createCommonAggregationselect(column, alias, tables);

        ConnectorSqlSelects selects = ConnectorSqlSelects.builder()
                .preprocessingSelects(commonAggSelect.getRootSelects())
                .aggregationSelect(commonAggSelect.getGroupBy())
                .build();

        String aggSelectPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
        Field<BigDecimal> qualifiedAvgSelect = commonAggSelect.getGroupBy().qualify(aggSelectPredecessor).select();
        RangeCondition condition = new RangeCondition(qualifiedAvgSelect, filterContext.getValue());

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

    private CommonAggregationSelect<BigDecimal> createCommonAggregationselect(
            Column column,
			String alias,
			ConnectorSqlTables tables) {

        Class<? extends Number> numberClass = NumberMapUtil.getType(column);

        ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), column.getName(), numberClass);

        String eventFilterCte = tables.cteName(ConceptCteStep.EVENT_FILTER);
        Field<? extends Number> avgField = rootSelect.qualify(eventFilterCte).select();
        FieldWrapper<BigDecimal> avgGroupBy = new FieldWrapper<>(DSL.avg(avgField).as(alias), column.getName());

        return CommonAggregationSelect.<BigDecimal>builder()
                .rootSelect(rootSelect)
                .groupBy(avgGroupBy)
                .build();
    }

}
