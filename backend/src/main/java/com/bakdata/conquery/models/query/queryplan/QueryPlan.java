package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.QueryPart;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;

public interface QueryPlan extends EventIterating {

	default QueryPlan createClone() {
		CloneContext ctx = new CloneContext();
		return this.clone(ctx);
	}
	
	QueryPlan clone(CloneContext ctx);

	default Stream<QueryPart> execute(QueryContext context, Collection<Entity> entries) {
		//collect required tables
		Set<Table> requiredTables = this.collectRequiredTables()
			.stream()
			.map(context.getStorage().getDataset().getTables()::getOrFail)
			.collect(Collectors.toSet());
		
		return entries
			.stream()
			.map(entity -> new QueryPart(context, this, requiredTables, entity));
	}

	EntityResult createResult();

	void addAggregator(Aggregator<?> aggregator);
	
	void addAggregator(int index, Aggregator<?> aggregator);

	SpecialDateUnion getSpecialDateUnion();

	void init(Entity entity);

	void nextEvent(Block block, int event);

	int getAggregatorSize();
	
	boolean isContained();
}