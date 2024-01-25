package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.QualifyingUtil;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Field;

class AggregationSelectCte extends ConceptCte {

	@Override
	public QueryStep.QueryStepBuilder convertStep(ConceptCteContext conceptCteContext) {

		String predecessor = conceptCteContext.getConceptTables().getPredecessor(ConceptCteStep.AGGREGATION_SELECT);
		Field<Object> primaryColumn = QualifyingUtil.qualify(conceptCteContext.getPrimaryColumn(), predecessor);

		List<SqlSelect> requiredInAggregationFilterStep = conceptCteContext.allConceptSelects()
																		   .flatMap(sqlSelects -> sqlSelects.getAggregationSelects().stream())
																		   .distinct()
																		   .toList();

		Selects aggregationSelectSelects = Selects.builder()
												  .primaryColumn(primaryColumn)
												  .sqlSelects(requiredInAggregationFilterStep)
												  .build();

		return QueryStep.builder()
						.selects(aggregationSelectSelects)
						.groupBy(List.of(primaryColumn));
	}

	@Override
	public ConceptCteStep cteStep() {
		return ConceptCteStep.AGGREGATION_SELECT;
	}

}
