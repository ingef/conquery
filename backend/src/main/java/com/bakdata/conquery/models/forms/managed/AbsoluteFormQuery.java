package com.bakdata.conquery.models.forms.managed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.Getter;

@Getter
@CPSType(id="ABSOLUTE_FORM_QUERY", base=QueryDescription.class)
public class AbsoluteFormQuery extends Query {

	/**
	 * see {@linkplain this#getResultInfos()}.
	 */
	public static final int FEATURES_OFFSET = 3;

	@NotNull
	@Valid
	private final Query query;
	@NotNull
	@Valid
	private final Range<LocalDate> dateRange;
	@NotNull
	@Valid
	private final ArrayConceptQuery features;
	@NotNull
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;

	public AbsoluteFormQuery(Query query, Range<LocalDate> dateRange, ArrayConceptQuery features, List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap) {
		this.query = query;
		this.dateRange = dateRange;
		this.features = features;
		this.resolutionsAndAlignmentMap = resolutionsAndAlignmentMap;
	}

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);
		features.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
	}

	@Override
	public AbsoluteFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new AbsoluteFormQueryPlan(
			query.createQueryPlan(context),
			new FormQueryPlan(
					DateContext.generateAbsoluteContexts(CDateRange.of(dateRange), resolutionsAndAlignmentMap),
					features.createQueryPlan(context),
					false
			)
		);
	}
	
	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();

		resultInfos.add(ConqueryConstants.RESOLUTION_INFO);
		resultInfos.add(ConqueryConstants.CONTEXT_INDEX_INFO);
		resultInfos.add(ConqueryConstants.DATE_RANGE_INFO);
		resultInfos.addAll(features.getResultInfos());

		return resultInfos;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
		features.visit(visitor);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return query.collectRequiredEntities(context);
	}
}
