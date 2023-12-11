package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.select.DateDistanceSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.MinSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;

public class DateDistanceSelectConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceSelectConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public SqlSelects convert(DateDistanceSelect dateDistanceSelect, SelectContext context) {

		NameGenerator nameGenerator = context.getNameGenerator();

		SqlSelect rootSelect = new DateDistanceSqlSelect(
				dateNowSupplier,
				dateDistanceSelect.getTimeUnit(),
				context.getConceptTables().getPredecessor(ConceptCteStep.PREPROCESSING),
				dateDistanceSelect.getColumn(),
				nameGenerator.selectName(dateDistanceSelect),
				context.getParentContext().getDateRestrictionRange(),
				context.getParentContext().getSqlDialect().getFunctionProvider()
		);

		Field<Object> qualifiedDateDistance = context.getConceptTables().qualifyOnPredecessor(ConceptCteStep.AGGREGATION_SELECT, rootSelect.aliased());
		MinSqlSelect minDateDistance = new MinSqlSelect(qualifiedDateDistance, nameGenerator.selectName(dateDistanceSelect));

		ExtractingSqlSelect<Object> firstValueReference = new ExtractingSqlSelect<>(
				context.getConceptTables().getPredecessor(ConceptCteStep.FINAL),
				minDateDistance.aliased().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(List.of(rootSelect))
						 .forAggregationSelectStep(List.of(minDateDistance))
						 .forFinalStep(List.of(firstValueReference))
						 .build();
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}

}
