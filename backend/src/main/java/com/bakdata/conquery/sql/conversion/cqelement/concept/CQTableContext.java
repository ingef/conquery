package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
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
@Builder
class CQTableContext implements Context {

	String conceptLabel;
	String conceptConnectorLabel;
	Field<Object> primaryColumn;
	Optional<ColumnDateRange> validityDate;
	List<SqlSelects> sqlSelects;
	List<SqlFilters> sqlFilters;
	ConnectorTables connectorTables;
	IntervalPackingContext intervalPackingContext;
	ConversionContext parentContext;
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
			return this.primaryColumn;
		}
		return previous.getSelects().getPrimaryColumn();
	}

	public Optional<IntervalPackingContext> getIntervalPackingContext() {
		return Optional.ofNullable(intervalPackingContext);
	}

	@Override
	public NameGenerator getNameGenerator() {
		return parentContext.getNameGenerator();
	}
}
