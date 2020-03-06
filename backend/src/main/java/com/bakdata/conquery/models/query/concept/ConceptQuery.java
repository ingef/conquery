package com.bakdata.conquery.models.query.concept;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.SecondaryIdNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = IQuery.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class ConceptQuery implements IQuery {

	@Valid
	@NotNull @NonNull
	protected CQElement root;
	protected String secondaryId;

	@Override
	public ConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = new ConceptQueryPlan(context);
		qp.setChild(root.createQueryPlan(context, qp));
		if(secondaryId != null) {
			qp.setChild(new SecondaryIdNode(secondaryId, qp.getChild()));
		}
		return qp;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public ConceptQuery resolve(QueryResolveContext context) {
		this.root = root.resolve(context);
		return this;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(ConqueryConstants.DATES_INFO);
		root.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		root.visit(visitor);
	}
}