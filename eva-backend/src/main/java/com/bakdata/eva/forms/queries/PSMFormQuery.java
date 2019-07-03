package com.bakdata.eva.forms.queries;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.eva.EvaConstants;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@CPSType(id="PSM_FORM_QUERY", base=IQuery.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class PSMFormQuery implements IQuery {

	@Getter @NotNull @NonNull
	private final RelativeFormQuery group;
	@Getter
	private final boolean constant;
	
	@Override
	public RelativeFormQueryPlan createQueryPlan(QueryPlanContext context) {
		RelativeFormQueryPlan plan = group.createQueryPlan(context);
		
		plan.addAggregator(0, new ConstantValueAggregator(
			constant,
			ResultType.BOOLEAN
		));
		
		return plan;
	}
	
	@Override
	public List<ResultInfo> collectResultInfos(PrintSettings config) {
		List<ResultInfo> header = group.collectResultInfos(config);
		header.add(0,new ResultInfo(EvaConstants.GROUP, ResultType.BOOLEAN, 0, 0));
		return header;
	}

	@Override
	public PSMFormQuery resolve(QueryResolveContext context) {
		return new PSMFormQuery(
			group.resolve(context),
			constant
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		requiredQueries.addAll(group.collectRequiredQueries());
	}
}
