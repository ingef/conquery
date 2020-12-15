package com.bakdata.conquery.models.forms.managed;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@Getter
@CPSType(id="ABSOLUTE_FORM_QUERY", base=QueryDescription.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator)
public class AbsoluteFormQuery extends IQuery {

	@NotNull @Valid
	private final IQuery query;
	@NotNull @Valid
	private final Range<LocalDate> dateRange;
	@NotNull @Valid
	private final ArrayConceptQuery features;
	@NotNull
	private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignmentMap;
	
	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);
	}

	@Override
	public AbsoluteFormQueryPlan createQueryPlan(QueryPlanContext context) {
		return new AbsoluteFormQueryPlan(
			query.createQueryPlan(context.withGenerateSpecialDateUnion(false)),
			DateContext.generateAbsoluteContexts(CDateRange.of(dateRange), resolutionsAndAlignmentMap),
			features.createQueryPlan(context.withGenerateSpecialDateUnion(false))
		);
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		features.collectResultInfos(collector);
		//remove SpecialDateUnion
		collector.getInfos().remove(0);

		collector.getInfos().add(0, ConqueryConstants.RESOLUTION_INFO);
		collector.getInfos().add(1, ConqueryConstants.CONTEXT_INDEX_INFO);
		collector.getInfos().add(2, ConqueryConstants.DATE_RANGE_INFO);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		query.visit(visitor);
		features.visit(visitor);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
	}
}
