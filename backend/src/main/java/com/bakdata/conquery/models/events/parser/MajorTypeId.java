package com.bakdata.conquery.models.events.parser;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.parser.specific.BooleanParser;
import com.bakdata.conquery.models.events.parser.specific.DateParser;
import com.bakdata.conquery.models.events.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.events.parser.specific.DecimalParser;
import com.bakdata.conquery.models.events.parser.specific.IntegerParser;
import com.bakdata.conquery.models.events.parser.specific.MoneyParser;
import com.bakdata.conquery.models.events.parser.specific.RealParser;
import com.bakdata.conquery.models.events.parser.specific.string.StringParser;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MajorTypeId {

	STRING(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new StringParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((StringStore) store).setString(offset, (int) value);
		}
	},
	INTEGER(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new IntegerParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((IntegerStore) store).setInteger(offset, (Long) value);
		}
	},
	BOOLEAN(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new BooleanParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((BooleanStore) store).setBoolean(offset, (boolean) value);
		}
	},
	REAL(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new RealParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((RealStore) store).setReal(offset, (Double) value);
		}
	},
	DECIMAL(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new DecimalParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((DecimalStore) store).setDecimal(offset, (BigDecimal) value);
		}
	},
	MONEY(false) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new MoneyParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((MoneyStore) store).setMoney(offset, (Long) value);
		}
	},
	DATE(true) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new DateParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((DateStore) store).setDate(offset, (Integer) value);
		}
	},
	DATE_RANGE(true) {
		@Override
		public Parser createParser(ParserConfig config) {
			return new DateRangeParser(config);
		}

		@Override
		public void set(int offset, Object value, ColumnStore store) {
			((DateRangeStore) store).setDateRange(offset, (CDateRange) value);
		}
	};

	@Getter
	private final boolean dateCompatible;


	public abstract Parser createParser(ParserConfig config);

	public abstract void set(int offset, Object value, ColumnStore store);

}
