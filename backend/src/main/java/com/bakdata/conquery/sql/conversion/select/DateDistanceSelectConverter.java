package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.DateDistanceSqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.MinSqlSelect;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;

public class DateDistanceSelectConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceSelectConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public SqlSelects convert(DateDistanceSelect dateDistanceSelect, SelectContext context) {


		SqlSelect rootSelect = new DateDistanceSqlSelect(
				dateNowSupplier,
				dateDistanceSelect.getTimeUnit(), context.getConceptTables().getPredecessorTableName(CteStep.PREPROCESSING),
				dateDistanceSelect.getColumn(),
				dateDistanceSelect.getName(),
				context.getParentContext().getDateRestrictionRange(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		Field<Object> qualifiedDateDistance = context.getConceptTables().qualifyOnPredecessorTableName(CteStep.AGGREGATION_SELECT, rootSelect.aliased());
		MinSqlSelect minDateDistance = new MinSqlSelect(qualifiedDateDistance, dateDistanceSelect.getName());

		ExtractingSqlSelect<Object> firstValueReference = new ExtractingSqlSelect<>(
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
