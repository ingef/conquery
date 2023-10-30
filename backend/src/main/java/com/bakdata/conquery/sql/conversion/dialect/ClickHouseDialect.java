package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.execution.CDateSetParser;
import org.jooq.DSLContext;

public class ClickHouseDialect implements SqlDialect {

	public final ClickHouseFunctionProvider CLICK_HOUSE_FUNCTION_PROVIDER;
	private final DSLContext dslContext;

	public ClickHouseDialect(DSLContext dslContext) {
		this.dslContext = dslContext;
		this.CLICK_HOUSE_FUNCTION_PROVIDER = new ClickHouseFunctionProvider();
	}

	@Override
	public DSLContext getDSLContext() {
		return this.dslContext;
	}

	@Override
	public CDateSetParser getCDateSetParser() {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.CLICK_HOUSE_FUNCTION_PROVIDER;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		throw new UnsupportedOperationException("Not implemented.");
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

}
