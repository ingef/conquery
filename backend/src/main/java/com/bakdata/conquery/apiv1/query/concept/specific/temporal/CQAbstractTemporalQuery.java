package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.PrecedenceMatcher;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract data class specifying the data needed for a TemporalQuery.
 */
@Getter
@AllArgsConstructor
public abstract class CQAbstractTemporalQuery extends CQElement {

	/**
	 * The query being executed as reference for preceding.
	 */
	@Getter
	protected final CQSampled index;

	/**
	 * The query being executed, compared to index. Events in preceding will be cut-off to be always before index, or at the same day, depending on the queries specific implementations.
	 */
	@Getter
	protected final CQSampled preceding;

	private QPNode preceedingPlan;
	private QPNode indexPlan;

	/**
	 * Create the {@link PrecedenceMatcher} specific to the implementing classes desired logic.
	 *
	 * @see PrecedenceMatcher
	 */
	protected abstract PrecedenceMatcher createMatcher();

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		indexPlan = index.getChild().createQueryPlan(context, plan);
		preceedingPlan = preceding.getChild().createQueryPlan(context, plan);

		return new Leaf();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		index.getChild().visit(visitor);
		preceding.getChild().visit(visitor);
	}
	
	@Override
	public void resolve(QueryResolveContext context) {
		index.getChild().resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		preceding.getChild().resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
	}
	
	@Override
	public List<ResultInfo> getResultInfos() {
		List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getChild().getResultInfos());
		resultInfos.addAll(preceding.getChild().getResultInfos());
		return resultInfos;
	}


	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().getChild().collectRequiredEntities(context); //TODO preceeding also?
	}
}
