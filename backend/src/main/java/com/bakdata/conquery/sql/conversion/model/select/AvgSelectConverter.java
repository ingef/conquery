package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.AvgSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.NumberMapUtil;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.math.BigDecimal;

public class AvgSelectConverter implements SelectConverter<AvgSelect> {

    @Override
    public ConnectorSqlSelects connectorSelect(final AvgSelect select, final SelectContext<ConnectorSqlTables> selectContext) {

        String alias = selectContext.getNameGenerator().selectName(select);
        ConnectorSqlTables tables = selectContext.getTables();
        Column avgColumn = select.getColumn().resolve();

        Class<? extends Number> numberClass = NumberMapUtil.getType(avgColumn);

        ExtractingSqlSelect<? extends Number> rootSelect = new ExtractingSqlSelect<>(tables.getRootTable(), avgColumn.getName(), numberClass);

//      value: concept_concept_connector-0-event_filter
        String eventFilterCte = tables.cteName(ConceptCteStep.EVENT_FILTER);

        Field<? extends Number> avgField = rootSelect
                .qualify(eventFilterCte) // change column from root table to event_filter
                .select();

        FieldWrapper<BigDecimal> avgGroupBy = new FieldWrapper<>(DSL.avg(avgField).as(alias), avgColumn.getName());

        String finalPredecessor = tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER);
        ExtractingSqlSelect<BigDecimal> finalSelect = avgGroupBy.qualify(finalPredecessor);

        return ConnectorSqlSelects.builder()
                .preprocessingSelect(rootSelect)
                .aggregationSelect(avgGroupBy)
                .finalSelect(finalSelect)
                .build();
    }

}
