package com.bakdata.conquery.sql.compiler.ir.concept;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/** Groups final concept selects and any auxiliary query step needed to calculate them. */
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
