package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;

public interface QueryPlan {

	QueryPlan clone(CloneContext ctx);

	EntityResult execute(QueryExecutionContext ctx, Entity entity);

	boolean isOfInterest(Entity entity);

	CDateSet getValidityDates(ContainedEntityResult result);
}