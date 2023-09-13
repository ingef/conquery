package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.MinGroupBy;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;

public class DateDistanceSelectConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceSelectConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public SqlSelects convert(DateDistanceSelect dateDistanceSelect, SelectContext context) {

		ConquerySelect rootSelect = new com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.DateDistanceSelect(
				dateNowSupplier,
				dateDistanceSelect.getTimeUnit(), context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				dateDistanceSelect.getColumn(),
				dateDistanceSelect.getName(),
				context.getParentContext().getDateRestrictionRange(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		Field<Object> qualifiedDateDistance = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.aliased());
		MinGroupBy minDateDistance = new MinGroupBy(qualifiedDateDistance, dateDistanceSelect.getName());

		ExtractingSelect<Object> firstValueReference = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				minDateDistance.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(rootSelect))
						 .forAggregationSelectStep(Collections.singletonList(minDateDistance))
						 .forFinalStep(Collections.singletonList(firstValueReference))
						 .build();
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}

}
