package com.bakdata.conquery.models.query.concept;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="CONCEPT_QUERY", base=IQuery.class)
public class ConceptQuery implements IQuery {
	
	@Valid @NotNull
	private CQElement root;
	
	@Override
	public QueryPlan createQueryPlan(CentralRegistry registry) {
		QueryPlan qp = QueryPlan.create();
		qp.setRoot(root.createQueryPlan(registry, qp));
		return qp;
	}
}