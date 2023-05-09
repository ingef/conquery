package com.bakdata.conquery.sql.conversion.select;

import java.time.LocalDate;
import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import org.jooq.Field;

public class SelectConverterService {

	private final List<SelectConverter<? extends Select>> converters = List.of(
			new DateDistanceConverter(LocalDate::now)
	);

	public Field<?> convertNode(Select selectNode, ConversionContext context) {
		return converters.stream()
						 .flatMap(converter -> converter.convert(selectNode, context).stream())
						 .findFirst()
						 .orElseThrow();
	}

}
