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

class JoinPredecessorsCte extends ConceptCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		List<QueryStep> queriesToJoin = new ArrayList<>();
		queriesToJoin.add(conceptCteContext.getPrevious());
		conceptCteContext.allConceptSelects()
						 .flatMap(sqlSelects -> sqlSelects.getAdditionalPredecessors().stream())
						 .forEach(queriesToJoin::add);

		Field<Object> primaryColumn = QueryStepJoiner.coalescePrimaryColumns(queriesToJoin);
		List<SqlSelect> mergedSelects = QueryStepJoiner.mergeSelects(queriesToJoin);
		Selects selects = Selects.builder()
								 .primaryColumn(primaryColumn)
								 .sqlSelects(mergedSelects)
								 .build();

		TableLike<Record> fromTable = QueryStepJoiner.constructJoinedTable(queriesToJoin, LogicalOperation.OR, conceptCteContext.getConversionContext());

		return QueryStep.builder()
						.selects(selects)
						.fromTable(fromTable)
						.predecessors(queriesToJoin);
	}

	@Override
	protected ConceptCteStep cteStep() {
		return ConceptCteStep.JOIN_PREDECESSORS;
	}

}
