package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.sql.conversion.model.LogicalOperation;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.QueryStepJoiner;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.TableLike;

class JoinPredecessorsCte extends ConnectorCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CQTableContext tableContext) {

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(tableContext.getPrevious());
		tableContext.allConceptSelects()
					.flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessors().stream())
					.forEach(queriesToJoin::add);

		Field<Object> primaryColumn = QueryStepJoiner.coalescePrimaryColumns(queriesToJoin);
		List<SqlSelect> mergedSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		Selects selects = Selects.builder()
								 .primaryColumn(primaryColumn)
								 .sqlSelects(mergedSelects)
								 .build();

		TableLike<Record> fromTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.AND, tableContext.getConversionContext());

		return QueryStep.builder()
						.selects(selects)
						.fromTable(fromTable)
						.predecessors(queriesToJoin);
	}

	@Override
	protected ConnectorCteStep cteStep() {
		return ConnectorCteStep.JOIN_PREDECESSORS;
	}

}
