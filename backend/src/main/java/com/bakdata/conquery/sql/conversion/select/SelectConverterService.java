package com.bakdata.conquery.sql.conversion.select;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.ConverterService;
import org.jooq.Field;

public class SelectConverterService extends ConverterService<Select, Field<?>> {

	public SelectConverterService(List<? extends SelectConverter<? extends Select>> converters) {
		super(converters);
	}
}
