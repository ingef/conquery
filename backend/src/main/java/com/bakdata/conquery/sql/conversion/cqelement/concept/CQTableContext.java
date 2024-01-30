package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelects;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jooq.Field;

@Value
@Builder(toBuilder = true)
class CQTableContext implements Context {

	ConversionContext conversionContext;
	String conceptLabel;
	Optional<ColumnDateRange> validityDate;
	boolean isExcludedFromDateAggregation;
	List<SqlSelects> sqlSelects;
	List<SqlFilters> sqlFilters;
	ConnectorTables connectorTables;
	@With
	QueryStep previous;

	/**
	 * @return All concepts {@link SqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public List<SqlSelects> allSqlSelects() {
		return Stream.concat(sqlSelects.stream(), sqlFilters.stream().map(SqlFilters::getSelects)).toList();
	}

	public Field<Object> getPrimaryColumn() {
		if (previous == null) {
			return conversionContext.getPrimaryColumn();
		}
		return previous.getQualifiedSelects().getPrimaryColumn();
	}

	@Override
	public NameGenerator getNameGenerator() {
		return conversionContext.getNameGenerator();
	}

}
