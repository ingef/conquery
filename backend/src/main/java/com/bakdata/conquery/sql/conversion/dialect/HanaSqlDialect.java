package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.AnsiSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.execution.DefaultSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.DSLContext;
import org.jooq.Field;

public class HanaSqlDialect implements SqlDialect {

	private final SqlFunctionProvider hanaSqlFunctionProvider;
	private final IntervalPacker hanaIntervalPacker;
	private final SqlDateAggregator hanaSqlDateAggregator;
	private final DefaultSqlCDateSetParser defaultNotationParser;

	public HanaSqlDialect() {
		this.hanaSqlFunctionProvider = new HanaSqlFunctionProvider();
		this.hanaIntervalPacker = new AnsiSqlIntervalPacker();
		this.hanaSqlDateAggregator = new AnsiSqlDateAggregator(this.hanaIntervalPacker);
		this.defaultNotationParser = new DefaultSqlCDateSetParser();
	}

	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.defaultNotationParser;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext dslContext) {
		return getDefaultNodeConverters(dslContext);
	}

	@Override
	public boolean isTypeCompatible(Field<?> field, MajorTypeId type) {
		return switch (type) {
			case STRING -> field.getDataType().isString();
			case INTEGER -> field.getDataType().isInteger();
			case BOOLEAN -> field.getDataType().isBoolean();
			case REAL -> field.getDataType().isNumeric();
			case DECIMAL -> field.getDataType().isDecimal();
			case MONEY -> field.getDataType().isDecimal();
			case DATE -> field.getDataType().isDate();
			//TODO is this available for Hana?
			case DATE_RANGE -> field.getDataType().getTypeName().equals("daterange");
		};
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.hanaSqlFunctionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return this.hanaIntervalPacker;
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		return this.hanaSqlDateAggregator;
	}

}
