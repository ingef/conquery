package com.bakdata.conquery.sql.conversion.dialect.hana;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.execution.DefaultResultSetProcessor;
import com.bakdata.conquery.sql.execution.HanaSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.Field;
import org.jooq.SQLDialect;

public class HanaDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final CompilerDialect compilerDialect;
	private final SqlCDateSetParser dateSetParser;

	public HanaDialectBundle() {
		this.functionProvider = new HanaSqlFunctionProvider();
		this.compilerDialect = new HanaCompilerDialect();
		this.dateSetParser = new HanaSqlCDateSetParser();
	}

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new DefaultResultSetProcessor(config, dateSetParser);
	}

	@Override
	public Dialect getDialect() {
		return Dialect.HANA;
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
	public CompilerDialect getCompilerDialect() {
		return compilerDialect;
	}

}
