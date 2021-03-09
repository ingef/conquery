package com.bakdata.conquery.models.query.concept.specific.temporal;

import java.util.function.Consumer;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * Abstract data class specifying the data needed for a TemporalQuery.
 */
@Getter
@AllArgsConstructor
public abstract class CQAbstractTemporalQuery extends CQElement {

	/**
	 * The query being executed as reference for preceding.
	 */
	protected final CQSampled index;

	/**
	 * The query being executed, compared to index. Events in preceding will be cut-off to be always before index, or at the same day, depending on the queries specific implementations.
	 */
	protected final CQSampled preceding;
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		index.getChild().visit(visitor);
		preceding.getChild().visit(visitor);
	}
	
	@Override
	public void resolve(QueryResolveContext context) {
		index.resolve(context.withDateAggregationMode(ConceptQueryPlan.DateAggregationMode.MERGE));
		preceding.resolve(context.withDateAggregationMode(ConceptQueryPlan.DateAggregationMode.MERGE));
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		index.getChild().collectResultInfos(collector);
		preceding.getChild().collectResultInfos(collector);
	}
}
