package com.bakdata.conquery.models.datasets.concepts.filters.postalcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.univocity.parsers.common.DataProcessingException;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.AbstractRowProcessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PostalCodeProcessor extends AbstractRowProcessor {
	final private List<Double> data = new ArrayList<>();
	private int recordsNumber = 0;
	final private List<String[]> notCorrectRows = new ArrayList<>();


	@Override
	public void processStarted(ParsingContext context) {
		super.processStarted(context);
		context.skipLines(1); //skip headers
	}

	@Override
	public void rowProcessed(String[] row, ParsingContext context) {
		try {
			data.addAll(Arrays.stream(row).map(Double::parseDouble).collect(Collectors.toList()));
			recordsNumber++;
		}
		catch (NumberFormatException err) {
			notCorrectRows.add(row);
			log.error("{}", err);
		}

	}
}
