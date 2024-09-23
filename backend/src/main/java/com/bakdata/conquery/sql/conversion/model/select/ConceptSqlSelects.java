package com.bakdata.conquery.sql.conversion.model.select;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ConceptSqlSelects {

	@Builder.Default
	Optional<QueryStep> additionalPredecessor = Optional.empty();

	@Singular
	List<SqlSelect> eventDateSelects;

	@Singular
	List<SqlSelect> finalSelects;

}
