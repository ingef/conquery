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
	public SqlSelects convert(DateDistanceSelect select, SelectContext context) {

		ConquerySelect dateDistanceSelect = new com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.DateDistanceSelect(
				dateNowSupplier,
				select.getTimeUnit(), context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				select.getColumn(),
				context.getParentContext().getDateRestrictionRange(),
				select.getLabel(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		Field<Object> qualifiedDateDistance = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, dateDistanceSelect.alias());
		MinGroupBy minDateDistance = new MinGroupBy(qualifiedDateDistance);

		ExtractingSelect<Object> firstValueReference = new ExtractingSelect<>(
				context.getConceptTables().getPredecessorTableName(CteStep.FINAL),
				minDateDistance.alias().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(dateDistanceSelect))
						 .forAggregationSelectStep(Collections.singletonList(minDateDistance))
						 .forFinalStep(Collections.singletonList(firstValueReference))
						 .build();
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}

}
