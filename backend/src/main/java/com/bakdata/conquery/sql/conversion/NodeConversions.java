package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.sql.compiler.conversion.ConversionDispatcher;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.compiler.naming.SqlNameGenerator;
import lombok.NonNull;
import org.jooq.DSLContext;

import java.time.Clock;
import java.util.Locale;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions implements NodeConversionDispatcher {

	private final ConversionDispatcher<Object, ConversionContext, ConversionContext> dispatcher;
	private final IdColumnConfig idColumns;
	private final LegacyCompilerDialect dialect;
	private final SqlNameGenerator nameGenerator;
	private final Clock clock;
	@NonNull
	private final String defaultPrimaryColumn;

	public NodeConversions(
			IdColumnConfig idColumns,
			LegacyCompilerDialect compilerDialect,
			DSLContext dslContext,
			Clock clock,
			String defaultPrimaryColumn
	) {
		this.dispatcher = new ConversionDispatcher<>(compilerDialect.getNodeConverters(dslContext));
		this.idColumns = idColumns;
		this.dialect = compilerDialect;
		this.nameGenerator = new SqlNameGenerator(compilerDialect.getNameMaxLength());
		this.clock = clock;
		this.defaultPrimaryColumn = defaultPrimaryColumn;
	}

	@Override
	public ConversionContext convert(Object node, ConversionContext context) {
		return dispatcher.convert(node, context);
	}

	public ConversionContext convert(QueryDescription queryDescription, ConqueryConfig conqueryConfig) {
		ConversionContext initialCtx = ConversionContext.builder()
				.idColumns(idColumns)
				.sqlPrintSettings(new PrintSettings(false, Locale.ROOT, conqueryConfig, null, null))
				.nameGenerator(nameGenerator)
				.nodeConversions(this)
				.clock(clock)
				.defaultPrimaryColumn(this.defaultPrimaryColumn)
				.stratificationFunctions(dialect.getStratificationFunctions())
				.compilerDialect(dialect)
				.build();
		return convert(queryDescription, initialCtx);
	}

}
