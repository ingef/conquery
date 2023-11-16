package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.AnsiSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.execution.DefaultSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.DSLContext;

public class HanaSqlDialect implements SqlDialect {

	private final SqlFunctionProvider hanaSqlFunctionProvider;
	private final IntervalPacker hanaIntervalPacker;
	private final SqlDateAggregator hanaSqlDateAggregator;
	private final DefaultSqlCDateSetParser defaultNotationParser;
	private final DSLContext dslContext;

	public HanaSqlDialect(DSLContext dslContext) {
		this.dslContext = dslContext;
		this.hanaSqlFunctionProvider = new HanaSqlFunctionProvider();
		this.hanaIntervalPacker = new AnsiSqlIntervalPacker();
		this.hanaSqlDateAggregator = new AnsiSqlDateAggregator(this.hanaSqlFunctionProvider, this.hanaIntervalPacker);
		this.defaultNotationParser = new DefaultSqlCDateSetParser();
	}

	@Override
	public DSLContext getDSLContext() {
		return this.dslContext;
	}

	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.defaultNotationParser;
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
		return this.hanaSqlFunctionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return this.hanaIntervalPacker;
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		return this.hanaSqlDateAggregator;
	}

}
