package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep.QueryStepBuilder;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import java.util.List;
import java.util.stream.Stream;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class NegationCte extends ConnectorCte {
    @Override
    protected ConceptCteStep cteStep() {
        return ConceptCteStep.NEGATION;
    }

    @Override
    protected QueryStepBuilder convertStep(CQTableContext tableContext) {
        ConnectorSqlTables connectorTables = tableContext.getConnectorTables();
        QueryStep aggregationFilterStep = tableContext.getPrevious();
        Selects aggregationFilterSelects = aggregationFilterStep.getQualifiedSelects();

        Table<Record> rootTable = DSL.table(DSL.name(connectorTables.getRootTable()));
        Table<Record> eventFilter = DSL.table(DSL.name(connectorTables.cteName(ConceptCteStep.EVENT_FILTER)));
        Table<Record> aggregationSelect = DSL.table(DSL.name(connectorTables.cteName(ConceptCteStep.AGGREGATION_SELECT)));
        Table<Record> aggregationFilter = DSL.table(DSL.name(connectorTables.cteName(ConceptCteStep.AGGREGATION_FILTER)));

        // predecessor is ID column without alias
        SqlIdColumns rootIds = tableContext.getIds().getPredecessor().get();
        SqlIdColumns eventFilterIds = tableContext.getIds().qualify(connectorTables.cteName(ConceptCteStep.EVENT_FILTER));
        SqlIdColumns aggregationSelectIds = tableContext.getIds().qualify(connectorTables.cteName(ConceptCteStep.AGGREGATION_SELECT));
        SqlIdColumns aggregationFilterIds = aggregationFilterSelects.getIds();

        Selects selects = Selects.builder()
                .ids(tableContext.getIds())
                .validityDate(aggregationFilterSelects.getValidityDate())
                .stratificationDate(aggregationFilterSelects.getStratificationDate())
                .sqlSelects(aggregationFilterSelects.getSqlSelects())
                .build();

        Condition negationCondition = Stream.concat(
                        eventFilterIds.toFields().stream().map(Field::isNull),
                        aggregationFilterIds.toFields().stream().map(Field::isNull)
                )
                .reduce(Condition::or)
                .orElseGet(DSL::noCondition);

        Table<Record> fromTable =
                rootTable.leftJoin(eventFilter).on(rootIds.join(eventFilterIds).toArray(Condition[]::new))
                        .leftJoin(aggregationSelect).on(rootIds.join(aggregationSelectIds).toArray(Condition[]::new))
                        .leftJoin(aggregationFilter).on(rootIds.join(aggregationFilterIds).toArray(Condition[]::new));

        return QueryStep.builder()
                .selects(selects)
                .conditions(List.of(negationCondition))
                .fromTable(fromTable)
                .predecessor(aggregationFilterStep);
    }

}
