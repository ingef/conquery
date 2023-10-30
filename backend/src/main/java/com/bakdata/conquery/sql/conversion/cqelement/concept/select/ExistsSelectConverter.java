package com.bakdata.conquery.sql.conversion.cqelement.concept.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.sql.conversion.model.select.ExistsSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;

public class ExistsSelectConverter implements SelectConverter<ExistsSelect> {

	@Override
	public SqlSelects convert(ExistsSelect existsSelect, SelectContext context) {
		String existsSelectAlias = context.getNameGenerator().selectName(existsSelect);
		return SqlSelects.builder()
						 .forFinalStep(List.of(new ExistsSqlSelect(existsSelect, existsSelectAlias)))
						 .build();
	}

	@Override
	public Class<? extends ExistsSelect> getConversionClass() {
		return ExistsSelect.class;
	}
}
