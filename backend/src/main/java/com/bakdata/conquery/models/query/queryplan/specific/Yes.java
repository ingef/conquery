package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class Yes extends QPNode {

	private final Table table;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
	}

	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		return true;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(table);
	}

	@Override
	public boolean isContained() {
		return true;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return Collections.emptySet();
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
}
