package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.PostgreSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.PostgreSqlIntervalPacker;
import com.bakdata.conquery.sql.execution.CDateSetParser;
import com.bakdata.conquery.sql.execution.DefaultNotationParser;
import org.jooq.DSLContext;

public class PostgreSqlDialect implements SqlDialect {

	private final SqlFunctionProvider postgresqlFunctionProvider;
	private final IntervalPacker postgresqlIntervalPacker;
	private final SqlDateAggregator postgresqlDateAggregator;
	private final DefaultNotationParser defaultNotationParser;
	private final DSLContext dslContext;

	public PostgreSqlDialect(DSLContext dslContext) {
		this.dslContext = dslContext;
		this.postgresqlFunctionProvider = new PostgreSqlFunctionProvider();
		this.postgresqlIntervalPacker = new PostgreSqlIntervalPacker(this.postgresqlFunctionProvider);
		this.postgresqlDateAggregator = new PostgreSqlDateAggregator(this.postgresqlFunctionProvider);
		this.defaultNotationParser = new DefaultNotationParser();
	}

	@Override
	public DSLContext getDSLContext() {
		return this.dslContext;
	}

	@Override
	public CDateSetParser getCDateSetParser() {
		return this.defaultNotationParser;
	}

	@Override
	public boolean requiresAggregationInFinalStep() {
		return false;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters() {
		return getDefaultNodeConverters();
	}

	@Override
	public List<FilterConverter<?, ?>> getFilterConverters() {
		return getDefaultFilterConverters();
	}

	@Override
	public List<SelectConverter<? extends Select>> getSelectConverters() {
		return getDefaultSelectConverters();
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.postgresqlFunctionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return this.postgresqlIntervalPacker;
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		return this.postgresqlDateAggregator;
	}

}
