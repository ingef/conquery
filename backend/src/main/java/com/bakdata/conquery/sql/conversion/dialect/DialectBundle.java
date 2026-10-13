package com.bakdata.conquery.sql.conversion.dialect;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import org.jooq.Field;
import org.jooq.SQLDialect;

/**
 * Aggregate used when wiring database-specific services for a backend namespace.
 *
 * <p>The compiler and SQL-function capabilities are exposed as separate services. Runtime wiring may retain the
 * bundle when it needs to construct compiler and execution services for the same database.</p>
 *
 * <p>This interface is an application composition boundary, not a compiler dependency.</p>
 */
public interface DialectBundle {

	CompilerDialect getCompilerDialect();

	SqlFunctionProvider getFunctionProvider();

	ResultSetProcessor getResultSetProcessor(ConqueryConfig config);

	Dialect getDialect();

	String getConnectionTestString();

	SQLDialect getJooqDialect();

	boolean isTypeCompatible(Field<?> field, MajorTypeId type);
}
