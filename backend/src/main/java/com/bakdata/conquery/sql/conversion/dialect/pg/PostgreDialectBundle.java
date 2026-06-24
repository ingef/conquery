package com.bakdata.conquery.sql.conversion.dialect.pg;

import static org.jooq.impl.SQLDataType.NVARCHAR;

import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.PostgreSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.PostgreSqlIntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.execution.DefaultResultSetProcessor;
import com.bakdata.conquery.sql.execution.PgSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.BuiltInDataType;
import org.jooq.postgres.extensions.types.DateRange;

@Slf4j
public class PostgreDialectBundle implements DialectBundle {

	private final SqlFunctionProvider functionProvider;
	private final IntervalPacker intervalPacker;
	private final SqlDateAggregator dateAggregator;
	private final SqlCDateSetParser dateSetParser;

	@Override
	public ResultSetProcessor getResultSetProcessor(ConqueryConfig config) {
		return new PgResultSetProcessor(config, dateSetParser);
	}

	@Override
	public Dialect getDialect() {
		return Dialect.POSTGRESQL;
	}

	@Override
	public int getNameMaxLength() {
		return 63;
	}

	@Override
	public String getConnectionTestString() {
		return "SELECT 1";
	}

	@Override
	public SQLDialect getJooqDialect() {
		return SQLDialect.POSTGRES;
	}

	public PostgreDialectBundle() {
		this.functionProvider = new PostgreSqlFunctionProvider();
		this.intervalPacker = new PostgreSqlIntervalPacker(this.functionProvider);
		this.dateAggregator = new PostgreSqlDateAggregator(this.functionProvider);
		this.dateSetParser = new PgSqlCDateSetParser();
	}




	@Override
	public boolean supportsSingleColumnRanges() {
		return true;
	}

	@Override
	public List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext dslContext) {
		return getDefaultNodeConverters(dslContext);
	}

	@Override
	public Map<Class<? extends Select>, ? extends SelectConverter<? extends Select>> getSelectConverterOverrides() {
		return Map.of(DistinctSelect.class, new PgDistinctSelectConverter());
	}

	@Override
	public StratificationFunctions getStratificationFunctions() {
		return new PostgresStratificationFunctions(((PostgreSqlFunctionProvider) getFunctionProvider()));
	}

	@Override
	public boolean isTypeCompatible(Field<?> field, MajorTypeId type) {
		log.trace("Field {} type: getTypeName={}, getQualifiedName={}", field.getName(), field.getDataType().getTypeName(), field.getDataType().getQualifiedName());
		return switch (type) {
			case STRING -> field.getDataType().isString();
			case INTEGER -> field.getDataType().isInteger();
			case BOOLEAN -> field.getDataType().isBoolean();
			case REAL -> field.getDataType().isNumeric();
			case DECIMAL -> field.getDataType().isDecimal();
			case MONEY -> field.getDataType().isNumeric(); // Not possible to introspect for
			case DATE -> field.getDataType().isDate();
			case DATE_RANGE -> field.getDataType().getSQLDataType().equals(new BuiltInDataType<>(DateRange.class, "daterange")); // Not possible to introspect for
		};
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

}
