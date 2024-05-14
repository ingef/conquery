package com.bakdata.conquery.sql.conversion.model.aggregator;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.select.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Getter;

/**
 * Container object for parts of the {@link ConnectorSqlSelects}.
 * <p>
 * {@link Select}s and {@link Filter}s like COUNT and SUM share the majority of their {@link ConnectorSqlSelects} when being converted. This container makes
 * the respective shared {@link SqlSelect}s and the optional additional preceding {@link QueryStep} accessible for building {@link ConnectorSqlSelects} on
 * demand.
 *
 * @param <T> The type parameter of the aggregation select.
 */
@Getter
class CommonAggregationSelect<T> {

	private final List<ExtractingSqlSelect<?>> rootSelects;
	private final FieldWrapper<T> groupBy;
	private final QueryStep additionalPredecessor;

	public CommonAggregationSelect(List<ExtractingSqlSelect<?>> rootSelects, FieldWrapper<T> groupBy, QueryStep additionalPredecessor) {
		this.rootSelects = rootSelects;
		this.groupBy = groupBy;
		this.additionalPredecessor = additionalPredecessor;
	}

	public CommonAggregationSelect(ExtractingSqlSelect<?> rootSelect, FieldWrapper<T> groupBy) {
		this.rootSelects = List.of(rootSelect);
		this.groupBy = groupBy;
		this.additionalPredecessor = null;
	}

	public CommonAggregationSelect(List<ExtractingSqlSelect<?>> rootSelects, FieldWrapper<T> groupBy) {
		this.rootSelects = rootSelects;
		this.groupBy = groupBy;
		this.additionalPredecessor = null;
	}

	public Optional<QueryStep> getAdditionalPredecessor() {
		return Optional.ofNullable(additionalPredecessor);
	}
}
