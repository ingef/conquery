package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.common.CDateSet;
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
		CDateSet dateSet = CDateSet.create();
		// Start with 1 for aggregator values, insert DateSet later at 0
		int resultInsertIdx= 1;
		for(ConceptQueryPlan child : childPlans) {
			EntityResult result = child.execute(ctx, entity);
			
			// Check if child 
			if(!result.isContained()) {
				// Advance pointer for the result insertion by the number of currently handled aggregators.
				resultInsertIdx = advanceResultPointer(resultInsertIdx, child);
				continue;
			}
			if (!(result instanceof SinglelineContainedEntityResult)) {
				throw new IllegalStateException(String.format(
					"Unhandled EntityResult Type %s: %s",
					result.getClass(),
					result.toString()));
			}
			SinglelineContainedEntityResult singleLineResult = (SinglelineContainedEntityResult) result;
			dateSet.addAll(prepareDateSet(singleLineResult.getValues()[0]));
			// Skip copying of first value: daterange
			int copyLength = calculateCopyLength(singleLineResult);
			System.arraycopy(singleLineResult.getValues(), 1, resultValues, resultInsertIdx, copyLength);			

			// Advance pointer for the result insertion by the number of currently handled aggregators.
			resultInsertIdx = advanceResultPointer(resultInsertIdx, child);
		}
		resultValues[0] = dateSet.toString();
		
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
		Integer size = childPlans.stream().map(ConceptQueryPlan::getAggregatorSize).reduce(0, Integer::sum);
		// Subtract the number of date aggregators (one per ConceptQueryPlan) and +1 for the whole query
		size -= childPlans.size();
		size += 1;
		return size;
	}
	
	private int advanceResultPointer(int resultInsertIdx, ConceptQueryPlan child) {
		int offset = child.getAggregatorSize()-1;
		if (offset < 0) {
			throw new IllegalStateException("Result index offset must be positive, so the advancing pointer does not override results.");
		}
		resultInsertIdx += offset;
		return resultInsertIdx;
	}
	
	private int calculateCopyLength(SinglelineContainedEntityResult singleLineResult) {
		int length = singleLineResult.getValues().length -1;
		if (length < 0) {
			throw new IllegalStateException("Copy length must be positive.");
		}
		return length;
	}
	
	private CDateSet prepareDateSet(Object value) {
		CDateSet range;
		range = CDateSet.parse(Objects.toString(value));
		return range;
	}
}
