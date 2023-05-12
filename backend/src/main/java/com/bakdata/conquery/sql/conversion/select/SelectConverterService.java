package com.bakdata.conquery.sql.conversion.select;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.ConverterService;
import org.jooq.Field;

public class SelectConverterService extends ConverterService<Select, Field<?>> {

	private static final List<? extends SelectConverter<? extends Select>> converters = List.of(
			new DateDistanceConverter(LocalDate::now)
	);

	public SelectConverterService() {
		super(converters);
	}
}
