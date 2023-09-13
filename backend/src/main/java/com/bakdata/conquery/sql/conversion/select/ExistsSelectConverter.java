package com.bakdata.conquery.sql.conversion.select;

import java.util.Collections;

import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExistsSelect;

public class ExistsSelectConverter implements SelectConverter<com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect> {

	@Override
	public SqlSelects convert(com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect convert, SelectContext context) {
		return SqlSelects.builder()
						 .forFinalStep(Collections.singletonList(new ExistsSelect(context.getLabel())))
						 .build();
	}

	@Override
	public Class<? extends com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect> getConversionClass() {
		return com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect.class;
	}
}
