package com.bakdata.conquery.sql.conversion.dialect;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import org.jooq.Field;
import org.jooq.SQLDialect;

/**
 * Compatibility aggregate used when wiring all database-specific services for a backend namespace.
 *
 * <p>The bundle exposes both the legacy compiler adapter inherited from {@link LegacyCompilerDialect} and the runtime
 * methods declared here. Legacy compiler code must depend on {@code LegacyCompilerDialect}, not on this aggregate.
 * Runtime wiring may retain the bundle when it needs to construct both compiler and execution services for the same
 * database.</p>
 *
 * <p>This interface keeps the existing HANA and ClickHouse integrations source-compatible while their compiler and
 * execution capabilities are separated. It is an application composition boundary, not a compiler dependency.</p>
 */
public interface DialectBundle extends LegacyCompilerDialect {

	ResultSetProcessor getResultSetProcessor(ConqueryConfig config);

	Dialect getDialect();

	String getConnectionTestString();

	SQLDialect getJooqDialect();

	boolean isTypeCompatible(Field<?> field, MajorTypeId type);
}
