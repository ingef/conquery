package com.bakdata.conquery.models.externalservice;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.bakdata.conquery.apiv1.forms.DateContextMode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.types.MajorTypeId;
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
				return NUMBER_FORMAT.format(((Number)f).longValue());
			}
			return f.toString();
		}
	},
	NUMERIC {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return DECIMAL_FORMAT.format(f);
			}
			return f.toString();
		}
	},
	CATEGORICAL,
	RESOLUTION {
		@Override
		public String print(PrintSettings cfg, @NonNull Object f) {
			DateContextMode mode = null;
			if(!( f instanceof DateContextMode)) {
				mode = DateContextMode.valueOf(f.toString());
			}
			else {
				mode = (DateContextMode) f;
			}
			Locale locale = cfg.getLocale();
			if(locale == null) {
				locale = Locale.getDefault();
			}
			ResourceBundle bundle = mode.getBundle(locale);
			return bundle.getString(f.toString());
		}
	},
	DATE,
	STRING,
	MONEY {
		@Override
		public String print(PrintSettings cfg, Object f) {
			if(cfg.isPrettyPrint()) {
				return DECIMAL_FORMAT.format(new BigDecimal(((Number)f).longValue()).movePointLeft(CURRENCY_DIGITS));
			}
			return INTEGER.print(cfg, f);
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
