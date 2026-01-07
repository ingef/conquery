package com.bakdata.conquery.sql.conversion.dialect;

import static org.jooq.impl.SQLDataType.NVARCHAR;

import java.util.List;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.aggregation.PostgreSqlDateAggregator;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.PostgreSqlIntervalPacker;
import com.bakdata.conquery.sql.execution.DefaultSqlCDateSetParser;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.postgresql.util.PGmoney;

@Slf4j
public class PostgreSqlDialect implements SqlDialect {

	private final SqlFunctionProvider postgresqlFunctionProvider;
	private final IntervalPacker postgresqlIntervalPacker;
	private final SqlDateAggregator postgresqlDateAggregator;
	private final DefaultSqlCDateSetParser defaultNotationParser;

	public PostgreSqlDialect() {
		this.postgresqlFunctionProvider = new PostgreSqlFunctionProvider();
		this.postgresqlIntervalPacker = new PostgreSqlIntervalPacker(this.postgresqlFunctionProvider);
		this.postgresqlDateAggregator = new PostgreSqlDateAggregator(this.postgresqlFunctionProvider);
		this.defaultNotationParser = new DefaultSqlCDateSetParser();
	}

	@Override
	public SqlCDateSetParser getCDateSetParser() {
		return this.defaultNotationParser;
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
	public boolean isTypeCompatible(Field<?> field, MajorTypeId type) {
		log.debug("Field {} type: getTypeName={}, getQualifiedName={}", field.getName(), field.getDataType().getTypeName(), field.getDataType().getQualifiedName());
		return switch (type) {
			case STRING -> field.getDataType().isString();
			case INTEGER -> field.getDataType().isInteger();
			case BOOLEAN -> field.getDataType().isBoolean();
			case REAL -> field.getDataType().isNumeric();
			case DECIMAL -> field.getDataType().isDecimal();
			case MONEY -> true; // TODO Need to find proper name
			case DATE -> field.getDataType().isDate();
			case DATE_RANGE -> true; // TODO Not properly fetched from postgres field.getDataType().getTypeName().equals("daterange");
		};
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
