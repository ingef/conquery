package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="CONCEPT_QUERY", base=IQuery.class)
public class ConceptQuery implements IQuery {
	
	@Valid @NotNull
	protected CQElement root;
	
	@Override
	public QueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = ConceptQueryPlan.create();
		qp.setChild(root.createQueryPlan(context, qp));
		return qp;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedQueryId> requiredQueries) {
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public ConceptQuery resolve(QueryResolveContext context) {
		this.root = root.resolve(context);
		return this;
	}
	
	public List<Select> collectSelects() {
		return root.collectSelects();
	}
	
	@Override
	public List<ResultInfo> collectResultInfos() {
		List<Select> selects = this.collectSelects();
		List<ResultInfo> header = new ArrayList<>(selects.size() + 1);
		header.add(ConqueryConstants.DATES_INFO);
		for(Select select : selects) {
			header.add(new ResultInfo(
				select.getLabel(),//select.getId().toStringWithoutDataset(),
				select.getResultType()
			));
		}
		return header;
	}
}