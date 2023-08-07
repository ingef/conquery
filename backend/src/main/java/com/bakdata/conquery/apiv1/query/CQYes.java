package com.bakdata.conquery.apiv1.query;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.Yes;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;

@CPSType(id = "YES", base = CQElement.class)
public class CQYes extends CQElement {

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		return new Yes(context.getDataset().getAllIdsTable());
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return Collections.emptyList();
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return new RequiredEntities(context.getBucketManager().getEntities().keySet());
	}
}
