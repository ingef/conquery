package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.PostgreSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.PostgreSqlIntervalPacker;
import com.bakdata.conquery.sql.conversion.forms.PostgresStratificationFunctions;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.execution.PgResultSetProcessor;
import com.bakdata.conquery.sql.execution.PostgreSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;

public class PostgreDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final IntervalPacker intervalPacker;
	private final SqlDateAggregator dateAggregator;
	private final SqlCDateSetParser dateSetParser;

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new PgResultSetProcessor(config, getCDateSetParser());
	}

	@Override
	public Dialect getDialect() {
		return Dialect.POSTGRESQL;
	}

	@Override
	public int getNameMaxLength() {
		return 63;
	}

	@Override
	public String getConnectionTestString() {
		return "SELECT 1";
	}

	@Override
	public SQLDialect getJooqDialect() {
		return SQLDialect.POSTGRES;
	}

	public PostgreDialectBundle() {
		this.functionProvider = new PostgreSqlFunctionProvider();
		this.intervalPacker = new PostgreSqlIntervalPacker(this.functionProvider);
		this.dateAggregator = new PostgreSqlDateAggregator(this.functionProvider);
		this.dateSetParser = new PostgreSqlCDateSetParser();
	}



	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.dateSetParser;
	}

	@Override
	public boolean supportsSingleColumnRanges() {
		return true;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext dslContext) {
		return getDefaultNodeConverters(dslContext);
	}

	@Override
	public StratificationFunctions getStratificationFunctions() {
		return new PostgresStratificationFunctions(((PostgreSqlFunctionProvider) getFunctionProvider()));
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
			case DATE_RANGE -> field.getDataType().getTypeName().equals("daterange");
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
