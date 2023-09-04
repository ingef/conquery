package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateDistanceSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.FirstValueGroupBy;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DateDistanceConverter implements SelectConverter<DateDistanceSelect> {

	private final DateNowSupplier dateNowSupplier;

	public DateDistanceConverter(DateNowSupplier dateNowSupplier) {
		this.dateNowSupplier = dateNowSupplier;
	}

	@Override
	public SqlSelects convert(DateDistanceSelect select, SelectContext context) {

		ConquerySelect dateDistanceSelect = new com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.DateDistanceSelect(
				dateNowSupplier,
				select.getTimeUnit(),
				context.getConceptTableNames().rootTable(),
				select.getColumn(),
				context.getParentContext().getDateRestrictionRange(),
				select.getLabel(),
				context.getParentContext().getSqlDialect().getFunction()
		);

		List<Field<?>> validityDateFields = context.getValidityDate()
												   .map(validityDate -> validityDate.qualify(context.getConceptTableNames().tableNameFor(CteStep.EVENT_FILTER))
																					.toFields())
												   .orElse(List.of(DSL.field(DSL.name(context.getParentContext().getConfig().getPrimaryColumn()))));

		FirstValueGroupBy firstValueGroupBy = new FirstValueGroupBy(
				context.getConceptTableNames().qualify(CteStep.EVENT_FILTER, dateDistanceSelect.alias()),
				validityDateFields,
				context.getParentContext().getSqlDialect().getFunction()
		);

		ExtractingSelect<Object> firstValueReference = new ExtractingSelect<>(
				context.getConceptTableNames().tableNameFor(CteStep.AGGREGATION_SELECT),
				firstValueGroupBy.alias().getName(),
				Object.class
		);

		return SqlSelects.builder()
						 .forPreprocessingStep(Collections.singletonList(dateDistanceSelect))
						 .forAggregationSelectStep(Collections.singletonList(firstValueGroupBy))
						 .forFinalStep(Collections.singletonList(firstValueReference))
						 .build();
	}

	@Override
	public Class<DateDistanceSelect> getConversionClass() {
		return DateDistanceSelect.class;
	}

}
