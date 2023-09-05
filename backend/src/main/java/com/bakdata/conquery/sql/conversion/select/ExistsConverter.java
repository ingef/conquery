package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.Exists;

public class ExistsConverter implements SelectConverter<ExistsSelect> {

	@Override
	public SqlSelects convert(ExistsSelect convert, SelectContext context) {
		return SqlSelects.builder()
						 .forFinalStep(Collections.singletonList(new Exists(context.getLabel())))
						 .build();
	}

	@Override
	public Class<? extends ExistsSelect> getConversionClass() {
		return ExistsSelect.class;
	}
}
