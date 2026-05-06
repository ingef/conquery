package com.bakdata.conquery.sql.conversion.dialect.clickhouse;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.AnsiSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.dialect.hana.HanaStratificationFunctions;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.select.ClickhouseDistinctSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;

@Slf4j
public class ClickhouseDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final IntervalPacker intervalPacker;
	private final SqlDateAggregator dateAggregator;
	private final SqlCDateSetParser dateSetParser;

	public ClickhouseDialectBundle() {
		this.functionProvider = new ClickhouseFunctionProvider();
		this.intervalPacker = new AnsiSqlIntervalPacker();
		this.dateAggregator = new AnsiSqlDateAggregator(this.intervalPacker, this.functionProvider);
		this.dateSetParser = new ClickhouseCDateSetParser(); // TODO => ArrayCDateSetParser
	}

	@Override
	public Dialect getDialect() {
		return Dialect.CLICKHOUSE;
	}

	@Override
	public int getNameMaxLength() {
		return 64;
	}

	@Override
	public String getConnectionTestString() {
		return "SELECT 1;";
	}

	@Override
	public SQLDialect getJooqDialect() {
		return SQLDialect.CLICKHOUSE;
	}

	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.dateSetParser;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext dslContext) {
		return getDefaultNodeConverters(dslContext);
	}

	@Override
	public StratificationFunctions getStratificationFunctions() {
		return new ClickhouseStratificationFunctions(getFunctionProvider());
	}

	@Override
	public boolean isTypeCompatible(Field<?> field, MajorTypeId type) {

		return true; //TODO CLickhouse integration is terrible here. We always receive just Object.
	}

	@Override
	public SqlFunctionProvider getFunctionProvider() {
		return this.functionProvider;
	}

	@Override
	public IntervalPacker getIntervalPacker() {
		return this.intervalPacker;
	}

	@Override
	public SqlDateAggregator getDateAggregator() {
		return this.dateAggregator;
	}

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new ClickhouseResultSetProcessor(config, getCDateSetParser());
	}

	@Override
	public Map<Class<? extends Select>, ? extends SelectConverter<? extends Select>> getSelectConverterOverrides() {
		return Map.of(DistinctSelect.class, new ClickhouseDistinctSelectConverter());
	}
}
