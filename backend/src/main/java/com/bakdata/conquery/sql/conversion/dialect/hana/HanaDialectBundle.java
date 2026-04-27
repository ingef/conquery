package com.bakdata.conquery.sql.conversion.dialect.hana;

import java.util.List;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.AnsiSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.execution.DefaultResultSetProcessor;
import com.bakdata.conquery.sql.execution.HanaSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;

public class HanaDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final IntervalPacker intervalPacker;
	private final SqlDateAggregator dateAggregator;
	private final SqlCDateSetParser dateSetParser;

	public HanaDialectBundle() {
		this.functionProvider = new HanaSqlFunctionProvider();
		this.intervalPacker = new AnsiSqlIntervalPacker();
		this.dateAggregator = new AnsiSqlDateAggregator(this.intervalPacker, functionProvider);
		this.dateSetParser = new HanaSqlCDateSetParser();
	}

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new DefaultResultSetProcessor(config, getCDateSetParser());
	}

	@Override
	public Dialect getDialect() {
		return Dialect.HANA;
	}

	@Override
	public int getNameMaxLength() {
		return 127;
	}

	@Override
	public String getConnectionTestString() {
		return "SELECT 1 FROM DUMMY";
	}

	@Override
	public SQLDialect getJooqDialect() {
		return SQLDialect.DEFAULT;
	}

	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.dateSetParser;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext dslContext) {
		return getDefaultNodeConverters(dslContext);
	}

	@Override
	public StratificationFunctions getStratificationFunctions() {
		return new HanaStratificationFunctions((HanaSqlFunctionProvider) getFunctionProvider());
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
			case DATE_RANGE -> false; // HANA does not support single-column DateRange
		};
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.functionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return this.intervalPacker;
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		return this.dateAggregator;
	}

}
