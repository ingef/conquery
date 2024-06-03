package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import org.jooq.DSLContext;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions extends Conversions<Visitable, ConversionContext, ConversionContext> {

	private final SqlDialect dialect;
	private final DatabaseConfig config;

	public NodeConversions(SqlDialect dialect, DSLContext dslContext, DatabaseConfig config) {
		super(dialect.getNodeConverters(dslContext));
		this.dialect = dialect;
		this.config = config;
	}

	public ConversionContext convert(QueryDescription queryDescription) {
		ConversionContext initialCtx = ConversionContext.builder()
														.config(config)
														.nameGenerator(new NameGenerator(config.getDialect().getNameMaxLength()))
														.nodeConversions(this)
														.sqlDialect(this.dialect)
														.build();
		return convert(queryDescription, initialCtx);
	}

}
