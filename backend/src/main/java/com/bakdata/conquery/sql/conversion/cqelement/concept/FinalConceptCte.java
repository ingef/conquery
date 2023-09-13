package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.context.selects.ConceptSelects;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelect;
import com.bakdata.conquery.sql.models.ColumnDateRange;

class FinalConceptCte extends ConceptCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(CteContext cteContext) {

		List<SqlSelect> finalSelects = cteContext.getSelects().stream()
												 .flatMap(sqlSelects -> sqlSelects.getForFinalStep().stream())
												 .distinct()
												 .collect(Collectors.toList());

		final Optional<ColumnDateRange> validityDate;
		if (cteContext.isExcludedFromDateAggregation()) {
			validityDate = Optional.empty();
		}
		else {
			validityDate =
					cteContext.getValidityDateRange()
							  .map(_validityDate -> _validityDate.qualify(cteContext.getConceptTables().getPredecessorTableName(CteStep.FINAL)));
		}

		return QueryStep.builder()
						.selects(new ConceptSelects(cteContext.getPrimaryColumn(), validityDate, finalSelects));
	}

	@Override
	protected CteStep cteStep() {
		return CteStep.FINAL;
	}

}
