package com.bakdata.conquery.sql.conversion;

import java.util.Locale;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import org.jooq.DSLContext;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions extends Conversions<Visitable, ConversionContext, ConversionContext> {

	private final IdColumnConfig idColumns;
	private final SqlDialect dialect;
	private final DatabaseConfig config;
	private final NameGenerator nameGenerator;
	private final SqlExecutionService executionService;

	public NodeConversions(
			IdColumnConfig idColumns,
			SqlDialect dialect,
			DSLContext dslContext,
			DatabaseConfig config,
			SqlExecutionService executionService
	) {
		super(dialect.getNodeConverters(dslContext));
		this.idColumns = idColumns;
		this.dialect = dialect;
		this.config = config;
		this.nameGenerator = new NameGenerator(config.getDialect().getNameMaxLength());
		this.executionService = executionService;
	}

	public ConversionContext convert(QueryDescription queryDescription, Namespace namespace, ConqueryConfig conqueryConfig) {
		ConversionContext initialCtx = ConversionContext.builder()
														.idColumns(idColumns)
														.sqlPrintSettings(new PrintSettings(false, Locale.ROOT, namespace, conqueryConfig, null, null, null))
														.config(config)
														.nameGenerator(nameGenerator)
														.nodeConversions(this)
														.sqlDialect(dialect)
														.executionService(executionService)
														.build();
		return convert(queryDescription, initialCtx);
	}

}
