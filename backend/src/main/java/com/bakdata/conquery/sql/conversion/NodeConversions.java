package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.NonNull;
import org.jooq.DSLContext;

import java.time.Clock;
import java.util.Locale;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions extends Conversions<Visitable, ConversionContext, ConversionContext> {

	private final IdColumnConfig idColumns;
	private final DialectBundle dialect;
	private final NameGenerator nameGenerator;
	private final SqlExecutionService executionService;
	private final Clock clock;
	@NonNull
	private final String defaultPrimaryColumn;

	public NodeConversions(
			IdColumnConfig idColumns,
			DialectBundle dialectBundle,
			DSLContext dslContext,
			SqlExecutionService executionService, Clock clock, String defaultPrimaryColumn
	) {
		super(dialectBundle.getNodeConverters(dslContext));
		this.idColumns = idColumns;
		this.dialect = dialectBundle;
		this.nameGenerator = new NameGenerator(dialectBundle.getNameMaxLength());
		this.executionService = executionService;
		this.clock = clock;
		this.defaultPrimaryColumn = defaultPrimaryColumn;
	}

	public ConversionContext convert(QueryDescription queryDescription, Namespace namespace, ConqueryConfig conqueryConfig) {
		ConversionContext initialCtx = ConversionContext.builder()
				.idColumns(idColumns)
				.sqlPrintSettings(new PrintSettings(false, Locale.ROOT, namespace, conqueryConfig, null, null))
				.nameGenerator(nameGenerator)
				.nodeConversions(this)
				.clock(clock)
				.defaultPrimaryColumn(this.defaultPrimaryColumn)
				.stratificationFunctions(dialect.getStratificationFunctions())
				.dialectBundle(dialect)
				.executionService(executionService)
				.build();
		return convert(queryDescription, initialCtx);
	}

}
