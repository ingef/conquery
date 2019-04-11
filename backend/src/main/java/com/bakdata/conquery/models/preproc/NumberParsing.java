package com.bakdata.conquery.models.preproc;

import java.math.BigDecimal;

import com.bakdata.conquery.models.exceptions.ParsingException;

public class NumberParsing {
	
	//see #150  parse with ConqueryConfig.getInstance().getLocale()
	
	public static long parseLong(String value) throws ParsingException {
		try {
			return Long.valueOf(value);
		} catch(Exception e) {
			throw ParsingException.of(value, "long", e);
		}
	}

	public static double parseDouble(String value) throws ParsingException {
		try {
			return Double.parseDouble(value);
		} catch(Exception e) {
			throw ParsingException.of(value, "double", e);
		}
	}

	public static BigDecimal parseBig(String value) throws ParsingException {
		try {
			return new BigDecimal(value);
		} catch(Exception e) {
			throw ParsingException.of(value, "BigDecimal", e);
		}
	}
	
	public static BigDecimal parseMoney(String value) throws ParsingException {
		try {
			return parseBig(value).stripTrailingZeros();
		} catch (ParsingException e) {
			throw ParsingException.of(value, "money", e);
		}
	}
}