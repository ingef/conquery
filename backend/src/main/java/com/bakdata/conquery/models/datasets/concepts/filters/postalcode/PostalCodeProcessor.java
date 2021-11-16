package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import lombok.Getter;

@Getter
public class PostalCodeProcessor extends AbstractRowProcessor {



	final private List<Double> data = new ArrayList<>();
	private int recordsNumber= 0;


	@Override
	public void rowProcessed(String[] row, ParsingContext context) {
		data.addAll(Arrays.stream(row).map(Double::parseDouble).collect(Collectors.toList()));
		recordsNumber++;
	}
}
