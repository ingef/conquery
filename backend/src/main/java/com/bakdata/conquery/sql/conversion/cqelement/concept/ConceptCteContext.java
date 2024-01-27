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
class ConceptCteContext implements Context {

	ConversionContext conversionContext;
	String conceptLabel;
	Optional<ColumnDateRange> validityDate;
	boolean isExcludedFromDateAggregation;
	List<SqlSelects> selects;
	List<SqlFilters> filters;
	ConceptTables conceptTables;
	@With
	QueryStep previous;

	/**
	 * @return All concepts {@link SqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public Stream<SqlSelects> allConceptSelects() {
		return Stream.concat(
				getFilters().stream().map(SqlFilters::getSelects),
				getSelects().stream()
		);
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
