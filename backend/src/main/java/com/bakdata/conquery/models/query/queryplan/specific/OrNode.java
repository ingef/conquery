package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QPParentNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;

public class OrNode extends QPParentNode {

	public OrNode(List<QPNode> children, DateAggregationAction action) {
		super(children, action);
	}
	
	private OrNode(List<QPNode> children, ListMultimap<Table, QPNode> childMap, DateAggregator dateAggregator) {
		super(children, childMap, dateAggregator);
	}

	
	@Override
	public QPNode doClone(CloneContext ctx) {
		Pair<List<QPNode>, ListMultimap<Table, QPNode>> fields = createClonedFields(ctx);
		return new OrNode(fields.getLeft(), fields.getRight(), getDateAggregator() != null ? ctx.clone(getDateAggregator()): null);
	}
	
	@Override
	public Optional<Boolean> aggregationFiltersApply() {
		Optional<Boolean> currently = Optional.empty();
		for(QPNode agg:getChildren()) {
			Optional<Boolean> result = agg.aggregationFiltersApply();
			if (!result.isPresent()) {
				// Undetermined
				continue;
			}
			if(result.get()){
				// TRUE
				return result;
			}
			// FALSE
			currently = result;
		}
		return currently;
	}
	
	public static QPNode of(Collection<QPNode> children, DateAggregationAction dateAggregationAction) {
		switch (children.size()) {
			case 0:
				return new Leaf();
			case 1:
				return children.iterator().next();
			default:
				return new OrNode(new ArrayList<>(children), dateAggregationAction);
		}
	}


	@Override
	public Optional<Boolean> eventFiltersApply(Bucket bucket, int event) {
		if (currentTableChildren.isEmpty()) {
			return Optional.empty();
		}
		boolean foundFalse = false;
		for (QPNode currentTableChild : currentTableChildren) {
			final Optional<Boolean> result = currentTableChild.eventFiltersApply(bucket, event);
			if (result.isEmpty()) {
				continue;
			}
			if (result.get()) {
				return result;
			}
			foundFalse = true;
		}
		return foundFalse ? Optional.of(Boolean.FALSE) : Optional.empty();
	}

}
