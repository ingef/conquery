package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.Visitable;
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

	public ConversionContext convert(QueryDescription queryDescription) {
		ConversionContext initialCtx = ConversionContext.builder()
														.idColumns(idColumns)
														.config(config)
														.nameGenerator(nameGenerator)
														.nodeConversions(this)
														.sqlDialect(dialect)
														.executionService(executionService)
														.build();
		return convert(queryDescription, initialCtx);
	}

}
