package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;

public class ExistsSelectConverter implements SelectConverter<ExistsSelect> {

	@Override
	public SqlSelects convert(ExistsSelect convert, SelectContext context) {
		return SqlSelects.builder()
						 .forFinalStep(Collections.singletonList(new ExistsSqlSelect(context.getLabel())))
						 .build();
	}

	@Override
	public Class<? extends ExistsSelect> getConversionClass() {
		return ExistsSelect.class;
	}
}
