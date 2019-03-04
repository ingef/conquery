package com.bakdata.conquery.models.externalservice;

import java.math.BigDecimal;
import java.text.NumberFormat;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.LocaleConfig;

import lombok.NonNull;

public enum ResultType {
	BOOLEAN {
		@Override
		public String print(Object f) {
			return Boolean.TRUE.equals(f) ? "t" : "f";			
		}
	},
	INTEGER {
		@Override
		public String print(Object f) {
			if(f instanceof Long) {
				return NUMBER_FORMAT.format((Long)f);
			}
			else {
				throw new IllegalArgumentException("Unknown type "+f.getClass()+" of INTEGER "+f);
			}
		}
	},
	NUMERIC {
		@Override
		public String print(Object f) {
			if(f instanceof Double) {
				return NUMBER_FORMAT.format((Double)f);
			}
			else if(f instanceof BigDecimal) {
				return NUMBER_FORMAT.format((BigDecimal)f);
			}
			else {
				throw new IllegalArgumentException("Unknown type "+f.getClass()+" of NUMERIC "+f);
			}
		}
	},
	CATEGORICAL,
	DATE,
	STRING,
	MONEY {
		@Override
		public String print(Object f) {
			if(f instanceof Long) {
				return CURRENCY_FORMAT.format(new BigDecimal((Long)f).divide(CURRENCY_DIGITS));
			}
			else {
				throw new IllegalArgumentException("Unknown type "+f.getClass()+" of MONEY "+f);
			}
		}
	};
	
	private final static NumberFormat NUMBER_FORMAT;
	private final static NumberFormat CURRENCY_FORMAT;
	private final static BigDecimal CURRENCY_DIGITS;
	
	
	static {
		LocaleConfig localeConfig = ConqueryConfig.getInstance().getLocale();
		
		NUMBER_FORMAT = NumberFormat.getNumberInstance(localeConfig.getNumberParsingLocale());
		CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(localeConfig.getNumberParsingLocale());
		CURRENCY_DIGITS = new BigDecimal(localeConfig.getCurrency().getDefaultFractionDigits());
	}
	
	public String printNullable(Object f) {
		if(f == null) {
			return "";
		}
		else {
			return print(f);
		}
	}
	
	public String print(@NonNull Object f) {
		return f.toString();
	}
}
