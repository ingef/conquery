package com.bakdata.conquery.models.preproc;

import java.math.BigDecimal;

import org.javamoney.moneta.FastMoney;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;

public class NumberParsing {
	
	//TODO parse with ConqueryConfig.getInstance().getLocale()
	
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
	
	public static FastMoney parseMoney(String value) throws ParsingException {
		try {
			return FastMoney
					.of(parseBig(value), ConqueryConfig.getInstance().getLocale().getCurrency())
					.stripTrailingZeros();
		} catch (ParsingException e) {
			throw ParsingException.of(value, "money", e);
		}
	}
}