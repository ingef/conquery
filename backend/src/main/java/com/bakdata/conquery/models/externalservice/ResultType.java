package com.bakdata.conquery.models.externalservice;


import java.math.BigDecimal;

import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.query.PrintSettings;
import lombok.NonNull;

public enum ResultType {
	BOOLEAN {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(f instanceof Boolean) {
				return (Boolean)f ? "t" : "f";
			}
			return "";
		}
	},
	INTEGER {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return cfg.getIntegerFormat().format(((Number)f).longValue());
			}
			return f.toString();
		}
	},
	NUMERIC {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(f);
			}
			return f.toString();
		}
	},
	CATEGORICAL,
	RESOLUTION {

		@Override
		public String print(PrintSettings cfg, Object f) {
			if (f instanceof DateContext.Resolution) {
				return ((DateContext.Resolution) f).toString(cfg.getLocale());
			}
			try {
				// If the object was parsed as a simple string, try to convert it to a
				// DateContextMode to get Internationalization
				return DateContext.Resolution.valueOf(f.toString()).toString(cfg.getLocale());
			}
			catch (Exception e) {
				throw new IllegalArgumentException(f + " is not a valid resolution.", e);
			}
		}
	},
	DATE,
	STRING,
	MONEY {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return cfg.getDecimalFormat().format(new BigDecimal(((Number)f).longValue()).movePointLeft(CURRENCY_DIGITS));
			}
			return INTEGER.print(cfg, f);
		}
	};


	private static final int CURRENCY_DIGITS = ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits();

	public String printNullable(PrintSettings cfg, Object f) {
		if (f == null) {
			return "";
		}
		return print(cfg, f);
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
