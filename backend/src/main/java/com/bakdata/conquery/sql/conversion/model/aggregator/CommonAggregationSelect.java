package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * Container object for parts of the {@link ConnectorSqlSelects}.
 * <p>
 * {@link Select}s and {@link Filter}s like COUNT and SUM share the majority of their {@link ConnectorSqlSelects} when being converted. This container makes
 * the respective shared {@link SqlSelect}s and the optional additional preceding {@link QueryStep} accessible for building {@link ConnectorSqlSelects} on
 * demand.
 *
 * @param <T> The type parameter of the aggregation select.
 */
@Value
@Builder
class CommonAggregationSelect<T> {

	@Singular
	List<SingleColumnSqlSelect> rootSelects;

	FieldWrapper<T> groupBy;

	QueryStep additionalPredecessor;

	public Optional<QueryStep> getAdditionalPredecessor() {
		return Optional.ofNullable(additionalPredecessor);
	}
}
