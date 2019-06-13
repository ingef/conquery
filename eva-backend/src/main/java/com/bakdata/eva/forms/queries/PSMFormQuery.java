package com.bakdata.eva.forms.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
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
	private final RelativeFormQuery controlGroup;
	@Getter @NotNull @NonNull
	private final RelativeFormQuery featureGroup;
	
	@Override
	public PSMFormQueryPlan createQueryPlan(QueryPlanContext context) {
		RelativeFormQueryPlan controlPlan = controlGroup.createQueryPlan(context);
		RelativeFormQueryPlan featurePlan = featureGroup.createQueryPlan(context);
		
		controlPlan.addAggregator(0, new ConstantValueAggregator(
			Boolean.TRUE,
			ResultType.BOOLEAN
		));
		featurePlan.addAggregator(0, new ConstantValueAggregator(
			Boolean.FALSE,
			ResultType.BOOLEAN
		));
		
		
		Set<TableId> required = controlPlan.collectRequiredTables();
		featurePlan.collectRequiredTables(required);
		
		return new PSMFormQueryPlan(
			controlPlan,
			featurePlan,
			new ArrayList<>(required)
		);
	}
	
	@Override
	public List<ResultInfo> collectResultInfos(PrintSettings config) {
		List<ResultInfo> header = controlGroup.collectResultInfos(config);
		header.add(0,new ResultInfo(EvaConstants.GROUP, ResultType.BOOLEAN, 0, 0));
		return header;
	}

	@Override
	public PSMFormQuery resolve(QueryResolveContext context) {
		return new PSMFormQuery(
			controlGroup.resolve(context),
			featureGroup.resolve(context)
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		requiredQueries.addAll(controlGroup.collectRequiredQueries());
		requiredQueries.addAll(featureGroup.collectRequiredQueries());
	}
}
