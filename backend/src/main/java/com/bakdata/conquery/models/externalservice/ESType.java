package com.bakdata.conquery.models.externalservice;

import java.text.DecimalFormatSymbols;

import org.apache.commons.lang3.NotImplementedException;

public enum ESType {
	BOOLEAN,
	INTEGER,
	NUMERIC {
		/*@Override
		public Field<String> print(Field<?> f) {
			return DSL.field(
				"trim(trailing ',' from trim(trailing '0 ' from to_char(({0})::decimal, '999999999999999990\""+StatisticType.DEZIMAL_SEPARATOR+"\"V9999999999999999999')))",
				String.class,
				f
			);
		}*/
		
	},
	CATEGORICAL {
		/*@Override
		public Field<String> print(Field<?> f) {
			return f.coerce(String.class);
		}*/
	},
	DATE,
	STRING {
		/*@Override
		public Field<String> print(Field<?> f) {
			return f.coerce(String.class);
		}*/
	},
	MONEY {
		/*@Override
		public Field<String> print(Field<?> f) {
			return DSL.field(
				"to_char({0},'FM999999999990\""+StatisticType.DEZIMAL_SEPARATOR+"\"99')",
				String.class,
				f
			);
		}*/
	};
	
	private final static char DEZIMAL_SEPARATOR = DecimalFormatSymbols.getInstance().getDecimalSeparator();
	
	public String print(Object o) {
		//see #153  printing logic for output to statistic form
		throw new NotImplementedException("");
	}
}
