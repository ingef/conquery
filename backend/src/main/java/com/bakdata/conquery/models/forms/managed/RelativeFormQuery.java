package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
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
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="RELATIVE_FORM_QUERY", base=QueryDescription.class)
@Getter
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class RelativeFormQuery extends Query {
	@NotNull @Valid
	private final Query query;
	@NotNull @Valid
	private final ArrayConceptQuery features;
	@NotNull
	private final TemporalSamplerFactory indexSelector;
	@NotNull
	private final IndexPlacement indexPlacement;
	@Min(0)
	private final int timeCountBefore;
	@Min(0)
	private final int timeCountAfter;
	@NotNull
	private final CalendarUnit timeUnit;
	@NotNull
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		features.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
	}

	@Override
	public RelativeFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new RelativeFormQueryPlan(
				query.createQueryPlan(context),
			// At the moment we do not use the dates of feature and outcome query
			features.createQueryPlan(context),
			indexSelector, indexPlacement, timeCountBefore,	timeCountAfter, timeUnit, resolutionsAndAlignmentMap);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
	}
	
	@Override
	public List<ResultInfo> getResultInfos() {
		List<ResultInfo> resultInfos = new ArrayList<>();
		// resolution
		resultInfos.add(ConqueryConstants.RESOLUTION_INFO);
		// index
		resultInfos.add(ConqueryConstants.CONTEXT_INDEX_INFO);
		// event date
		resultInfos.add(ConqueryConstants.EVENT_DATE_INFO);
		// date range info
		resultInfos.add(ConqueryConstants.DATE_RANGE_INFO);

		final List<ResultInfo> featureInfos = features.getResultInfos();

		resultInfos.add(ConqueryConstants.OBSERVATION_SCOPE_INFO);

		//features
		resultInfos.addAll(featureInfos);

		return resultInfos;
	}


	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
		features.visit(visitor);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return query.collectRequiredEntities(context);
	}
}
