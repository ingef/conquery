package com.bakdata.conquery.sql.conversion.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Conversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;

public class SelectConversions extends Conversions<Select, SqlSelects, SelectContext> {

	public SelectConversions(List<? extends SelectConverter<? extends Select>> converters) {
		super(converters);
	}

}
