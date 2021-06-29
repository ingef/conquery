package com.bakdata.conquery.models.forms.managed;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.IndexPlacement;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="RELATIVE_FORM_QUERY", base=QueryDescription.class)
@Getter
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class RelativeFormQuery extends Query {
	@NotNull @Valid
	private final Query query;
	@NotNull @Valid
	private final ArrayConceptQuery features;
	@NotNull @Valid
	private final ArrayConceptQuery outcomes;
	@NotNull
	private final TemporalSampler indexSelector;
	@NotNull
	private final IndexPlacement indexPlacement;
	@Min(0)
	private final int timeCountBefore;
	@Min(0)
	private final int timeCountAfter;
	@NotNull
	private final DateContext.CalendarUnit timeUnit;
	@NotNull
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;
	
	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		features.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
		outcomes.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
	}
	
	@Override
	public RelativeFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new RelativeFormQueryPlan(query.createQueryPlan(context),
			// At the moment we do not use the dates of feature and outcome query
			features.createQueryPlan(context),
			outcomes.createQueryPlan(context),
			indexSelector, indexPlacement, timeCountBefore,	timeCountAfter, timeUnit, resolutionsAndAlignmentMap);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
		outcomes.collectRequiredQueries(requiredQueries);
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		// resolution
		collector.add(ConqueryConstants.RESOLUTION_INFO);
		// index
		collector.add(ConqueryConstants.CONTEXT_INDEX_INFO);
		// event date
		collector.add(ConqueryConstants.EVENT_DATE_INFO);

		final List<ResultInfo> featureInfos = features.collectResultInfos().getInfos();
		final List<ResultInfo> outcomeInfos = outcomes.collectResultInfos().getInfos();

		//date ranges
		if (!featureInfos.isEmpty()){
			collector.add(ConqueryConstants.FEATURE_DATE_RANGE_INFO);
		}

		if (!outcomeInfos.isEmpty()) {
			collector.add(ConqueryConstants.OUTCOME_DATE_RANGE_INFO);
		}
		//features
		collector.addAll(featureInfos);
		collector.addAll(outcomeInfos);
	}
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
		outcomes.visit(visitor);
		features.visit(visitor);
	}
}
