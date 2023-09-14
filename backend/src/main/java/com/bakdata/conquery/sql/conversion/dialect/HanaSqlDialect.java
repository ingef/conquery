package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.AnsiSqlIntervalPacker;
import org.jooq.DSLContext;

public class HanaSqlDialect implements SqlDialect {

	private final SqlFunctionProvider hanaSqlFunctionProvider;
	private final IntervalPacker hanaIntervalPacker;
	private final DSLContext dslContext;

	public HanaSqlDialect(DSLContext dslContext) {
		this.dslContext = dslContext;
		this.hanaSqlFunctionProvider = new HanaSqlFunctionProvider();
		this.hanaIntervalPacker = new AnsiSqlIntervalPacker(this.hanaSqlFunctionProvider);
	}

	@Override
	public DSLContext getDSLContext() {
		return this.dslContext;
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
		return hanaSqlFunctionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return hanaIntervalPacker;
	}

}
