package com.bakdata.conquery.sql.conversion;

import java.util.Locale;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import org.jooq.DSLContext;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions extends Conversions<Visitable, ConversionContext, ConversionContext> {

	private final IdColumnConfig idColumns;
	private final DialectBundle dialect;
	private final NameGenerator nameGenerator;
	private final SqlExecutionService executionService;
	private final DateNowSupplier dateNowSupplier;

	public NodeConversions(
			IdColumnConfig idColumns,
			DialectBundle dialect,
			DSLContext dslContext,
			SqlExecutionService executionService, DateNowSupplier dateNowSupplier
	) {
		super(dialect.getNodeConverters(dslContext));
		this.idColumns = idColumns;
		this.dialect = dialect;
		this.nameGenerator = new NameGenerator(dialect.getDialect().getNameMaxLength());
		this.executionService = executionService;
		this.dateNowSupplier = dateNowSupplier;
	}

	public ConversionContext convert(QueryDescription queryDescription, Namespace namespace, ConqueryConfig conqueryConfig) {
		ConversionContext initialCtx = ConversionContext.builder()
														.idColumns(idColumns)
														.sqlPrintSettings(new PrintSettings(false, Locale.ROOT, namespace, conqueryConfig, null, null))
														.nameGenerator(nameGenerator)
														.nodeConversions(this)
														.dateNowSupplier(dateNowSupplier)
														.dialectBundle(dialect)
														.executionService(executionService)
														.build();
		return convert(queryDescription, initialCtx);
	}

}
