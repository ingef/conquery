package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.Optional;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterContext<V> implements Context {

	SqlIdColumns ids;

	/**
	 * A filter value ({@link FilterValue#getValue()})
	 */
	V value;

	ConversionContext conversionContext;

	/**
	 * Not present if this context is for table export.
	 */
	@Nullable
	ConnectorSqlTables tables;

	public static <V> FilterContext<V> forConceptConversion(SqlIdColumns ids, V value, ConversionContext conversionContext, ConnectorSqlTables tables) {
		return new FilterContext<>(ids, value, conversionContext, tables);
	}

	public static <V> FilterContext<V> forTableExport(SqlIdColumns ids, V value, ConversionContext conversionContext) {
		return new FilterContext<>(ids, value, conversionContext, null);
	}

	public SqlFunctionProvider getFunctionProvider() {
		return getSqlDialect().getFunctionProvider();
	}

	public SqlIdColumns getIds(CteStep cteStep) {
		if (!conversionContext.isWithStratification()) {
			return ids;
		}
		return conversionContext.getStratificationTable().getQualifiedSelects().getIds().qualify(tables.cteName(cteStep));
	}

	public Optional<ColumnDateRange> getStratificationDate(CteStep cteStep) {
		return Optional.ofNullable(conversionContext.getStratificationTable())
					   .flatMap(stratificationTable -> stratificationTable.getQualifiedSelects().getStratificationDate())
					   .map(stratificationDate -> stratificationDate.qualify(tables.cteName(cteStep)));
	}

}
