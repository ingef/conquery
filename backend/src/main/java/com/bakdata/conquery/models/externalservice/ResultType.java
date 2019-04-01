package com.bakdata.conquery.models.externalservice;

import java.math.BigDecimal;
import java.text.NumberFormat;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.NonNull;

public enum ResultType {
	BOOLEAN {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(f instanceof Boolean)
				return (Boolean)f ? "t" : "f";
			else
				return "";
		}
	},
	INTEGER {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return NUMBER_FORMAT.format(((Number)f).longValue());
			}
			else {
				return f.toString();
			}
		}
	},
	NUMERIC {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return DECIMAL_FORMAT.format(f);
			}
			else {
				return f.toString();
			}
		}
	},
	CATEGORICAL,
	DATE,
	STRING,
	MONEY {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return DECIMAL_FORMAT.format(new BigDecimal(((Number)f).longValue()).movePointLeft(CURRENCY_DIGITS));
			}
			else {
				return INTEGER.print(cfg, f);
			}
		}
	};
	
	
	
	
	
	
	

	private static final NumberFormat NUMBER_FORMAT;
	private static final NumberFormat DECIMAL_FORMAT;
	private static final int CURRENCY_DIGITS;

	static {
		LocaleConfig localeConfig = ConqueryConfig.getInstance().getLocale();

		NUMBER_FORMAT = NumberFormat.getNumberInstance(localeConfig.getNumberParsingLocale());
		DECIMAL_FORMAT = NumberFormat.getNumberInstance(localeConfig.getNumberParsingLocale());
		DECIMAL_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
		CURRENCY_DIGITS = localeConfig.getCurrency().getDefaultFractionDigits();
	}

	public String printNullable(PrintSettings cfg, Object f) {
		if (f == null) {
			return "";
		}
		else {
			return print(cfg, f);
		}
	}
	
	public String print(PrintSettings cfg, @NonNull Object f) {
		return f.toString();
	}
	
	public static ResultType resolveResultType(MajorTypeId majorTypeId) {
		switch (majorTypeId) {
			case STRING:
				return ResultType.STRING;
			case BOOLEAN:
				return ResultType.BOOLEAN;
			case DATE:
				return ResultType.DATE;
			case DATE_RANGE:
				return ResultType.STRING;
			case INTEGER:
				return ResultType.INTEGER;
			case MONEY:
				return ResultType.MONEY;
			case DECIMAL:
			case REAL:
				return ResultType.NUMERIC;
			default:
				throw new IllegalStateException(String.format("Invalid column type '%s'", majorTypeId));
		}
	}
}
