package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.QueryPart;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.results.EntityResult;

public interface QueryPlan<SELF extends QueryPlan<SELF>> extends Cloneable, EventIterating {

	SELF clone();

	Stream<QueryPart> execute(QueryContext context, Collection<Entity> entries);

	EntityResult createResult();

	<T> Aggregator<T> getCloneOf(QueryPlan<?> originalPlan, Aggregator<T> aggregator);

	void addAggregator(Aggregator<?> aggregator);

	SpecialDateUnion getSpecialDateUnion();

}