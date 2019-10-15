package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ArrayQueryPlan implements QueryPlan, EventIterating {
	List<ConceptQueryPlan> childPlans = new ArrayList<>();

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return childPlans.stream().map(cp -> cp.isOfInterest(bucket)).reduce(Boolean::logicalOr).orElse(false);
	}

	@Override
	public QueryPlan clone(CloneContext ctx) {
		List<ConceptQueryPlan> childClones = childPlans.stream().map(qp -> qp.clone(ctx)).collect(Collectors.toList());
		ArrayQueryPlan aqClone = new ArrayQueryPlan();
		aqClone.setChildPlans(childClones);
		return aqClone;
	}

	@Override
	public EntityResult execute(QueryContext ctx, Entity entity) {
		Object[] resultValues = new Object[this.getAggregatorSize()];
		int resultInsertIdx= 0;
		for(ConceptQueryPlan child : childPlans) {
			EntityResult result = child.execute(ctx, entity);
			
			// Check if child 
			if(result.isContained()) {
				// Advance pointer for the result insertion by the number of currently handled aggregartors.
				resultInsertIdx += child.getAggregatorSize();
				continue;
			}
			if (!(result instanceof SinglelineContainedEntityResult)) {
				throw new IllegalStateException(String.format(
					"Unhandled EntityResult Type %s: %s",
					result.getClass(),
					result.toString()));
			}
			SinglelineContainedEntityResult singleLineResult = (SinglelineContainedEntityResult) result;
			System.arraycopy(singleLineResult.getValues(), 0, resultValues, resultInsertIdx, singleLineResult.getValues().length);			
			
			// Advance pointer for the result insertion by the number of currently handled aggregartors.
			resultInsertIdx += child.getAggregatorSize();
		}
		
		return new SinglelineContainedEntityResult(entity.getId(), resultValues);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return childPlans.stream().map(cp -> cp.isOfInterest(entity)).reduce(Boolean::logicalOr).orElse(false);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		for(ConceptQueryPlan child : childPlans) {
			Set<TableId> tables = new HashSet<>();
			child.collectRequiredTables(tables);
			requiredTables.addAll(tables);
		}
	}
	
	private int getAggregatorSize() {
		return childPlans.stream().map(ConceptQueryPlan::getAggregatorSize).reduce(0, Integer::sum);
	}
}
