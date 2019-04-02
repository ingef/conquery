package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
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

		Map<SelectId, Boolean> collisions = new HashMap<>();

		// find all select ids that occur multiple times
		for(Select select : selects) {
			collisions.compute(select.getId(), (key, value) -> value != null);
		}

		Map<SelectId, Integer> occurences = new HashMap<>();

		for(Select select : selects) {
			final Integer occurence = occurences.compute(select.getId(), (id, n) -> n == null ? 0 : n + 1);

			if (!collisions.getOrDefault(select.getId(), false)) {
				header.add(new ResultInfo(select.getId().toStringWithoutDataset(), select.getResultType()));
			}
			else {
				header.add(new ResultInfo(select.getId().toStringWithoutDataset() + "_" + occurence, select.getResultType()));
			}
		}
		return header;
	}
}