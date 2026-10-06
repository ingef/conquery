package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.dialect.clickhouse.ClickhouseCompilerDialect;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Field;
import org.jooq.SQLDialect;

@Slf4j
public class ClickhouseDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final CompilerDialect compilerDialect;

	public ClickhouseDialectBundle() {
		this.functionProvider = new ClickhouseFunctionProvider();
		this.compilerDialect = new ClickhouseCompilerDialect();
	}

	@Override
	public Dialect getDialect() {
		return Dialect.CLICKHOUSE;
	}

	@Override
	public String getConnectionTestString() {
		return "SELECT 1;";
	}

	@Override
	public SQLDialect getJooqDialect() {
		return SQLDialect.CLICKHOUSE;
	}

	@Override
	public boolean isTypeCompatible(Field<?> field, MajorTypeId type) {
		return true; //TODO CLickhouse integration is terrible here. We always receive just Object.
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.functionProvider;
	}

	@Override
	public CompilerDialect getCompilerDialect() {
		return compilerDialect;
	}

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new ClickhouseResultSetProcessor(config);
	}

}
