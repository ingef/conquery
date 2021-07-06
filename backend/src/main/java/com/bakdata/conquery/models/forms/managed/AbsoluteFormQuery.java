package com.bakdata.conquery.models.forms.managed;

import java.time.LocalDate;
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
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@CPSType(id="ABSOLUTE_FORM_QUERY", base=QueryDescription.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class AbsoluteFormQuery extends Query {

	@NotNull @Valid
	private final Query query;
	@NotNull @Valid
	private final Range<LocalDate> dateRange;
	@NotNull @Valid
	private final ArrayConceptQuery features;
	@NotNull
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;
	
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
					features.createQueryPlan(context))
		);
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		features.collectResultInfos(collector);

		collector.getInfos().add(0, ConqueryConstants.RESOLUTION_INFO);
		collector.getInfos().add(1, ConqueryConstants.CONTEXT_INDEX_INFO);
		collector.getInfos().add(2, ConqueryConstants.DATE_RANGE_INFO);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
		features.visit(visitor);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
	}
}
