package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConceptFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.SqlSelects;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jooq.Field;

@Value
@Builder(toBuilder = true)
class CteContext implements Context {

	ConversionContext context;
	Field<Object> primaryColumn;
	Optional<ColumnDateRange> validityDateRange;
	boolean isExcludedFromDateAggregation;
	List<SqlSelects> selects;
	List<ConceptFilter> filters;
	ConceptTableNames conceptTableNames;
	String conceptLabel;
	@With
	QueryStep previous;

	/**
	 * @return All concepts {@link SqlSelects} that are either required for {@link Filter}'s or {@link Select}'s.
	 */
	public Stream<SqlSelects> allConceptSelects() {
		return Stream.concat(
				getFilters().stream().map(ConceptFilter::getSelects),
				getSelects().stream()
		);
	}

}
