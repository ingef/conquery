package com.bakdata.eva.forms.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.ResultInfo;
import com.bakdata.eva.EvaConstants;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@CPSType(id="RELATIVE_FORM_QUERY", base=IQuery.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class RelativeFormQuery implements IQuery {

	@Getter @NotNull @NonNull
	private final FormQuery featureQuery;
	@Getter @NotNull @NonNull
	private final FormQuery outcomeQuery;

	@Override
	public RelativeFormQuery resolve(QueryResolveContext context) {
		return new RelativeFormQuery(
			featureQuery.resolve(context),
			outcomeQuery.resolve(context)
		);
	}
	@Override
	public RelativeFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new RelativeFormQueryPlan(
			featureQuery.createQueryPlan(context),
			outcomeQuery.createQueryPlan(context)
		);
	}
	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		featureQuery.collectRequiredQueries(requiredQueries);
		outcomeQuery.collectRequiredQueries(requiredQueries);
	}
	@Override
	public List<ResultInfo> collectResultInfos(PrintSettings config) {
		List<ResultInfo> featureHeader = featureQuery.collectResultInfos(config);
		List<ResultInfo> outcomeHeader = outcomeQuery.collectResultInfos(config);

		List<ResultInfo> list = new ArrayList<>();
		//index
		list.add(featureHeader.get(0));
		// event date
		list.add(EvaConstants.EVENT_DATE_INFO);
		//date ranges
		list.add(prefixName(featureHeader.get(1), EvaConstants.FEATURE_PREFIX));
		list.add(prefixName(outcomeHeader.get(1), EvaConstants.OUTCOME_PREFIX));
		//features
		list.addAll(
			featureHeader
				.subList(2, featureHeader.size())
				.stream()
				.map(v->prefixName(v, EvaConstants.FEATURE_PREFIX))
				.collect(Collectors.toList())
		);
		list.addAll(
			outcomeHeader
				.subList(2, outcomeHeader.size())
				.stream()
				.map(v->prefixName(v, EvaConstants.OUTCOME_PREFIX))
				.collect(Collectors.toList())
		);
		return list;
	}
	
	private static ResultInfo prefixName(ResultInfo info, String prefix) {
		return info.withName(prefix + info.getName());
	}
}
