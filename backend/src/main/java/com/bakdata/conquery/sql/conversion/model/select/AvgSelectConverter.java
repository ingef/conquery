package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.AvgSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public class AvgSelectConverter implements SelectConverter<AvgSelect> {

    @Override
    public ConnectorSqlSelects connectorSelect(final AvgSelect select, final SelectContext<ConnectorSqlTables> selectContext) {
        // TODO implement conversion
        // Aggregation step is similar to CountSqlAggregator/SumSqlAggregator
        return null;
    }


}
