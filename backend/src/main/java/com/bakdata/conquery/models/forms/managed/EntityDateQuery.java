package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This query uses the date range defined by {@link EntityDateQuery#query} for each entity and applies it as a
 * date restriction for the following query defined by {@link EntityDateQuery#features}.
 * The additional {@link EntityDateQuery#dateRange} is applied globally on all entities.
 */
@CPSType(id = "ENTITY_DATE_QUERY", base = QueryDescription.class)
@Getter
@RequiredArgsConstructor
public class EntityDateQuery extends Query {

    @NotNull
    @Valid
    private final Query query;
    @NotNull @Valid
    private final ArrayConceptQuery features;

    @NotNull @NotEmpty
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;

    @NotNull @Valid
    private final CDateRange dateRange;

    @NotNull
    private final DateAggregationMode dateAggregationMode;


    @Override
    public EntityDateQueryPlan createQueryPlan(QueryPlanContext context) {
        // Clear all selects we need only the date union which is enforced through the content
        Visitable.stream(query)
				 .filter(CQConcept.class::isInstance)
				 .map(CQConcept.class::cast)
				 .forEach(concept -> {
					 concept.setSelects(Collections.emptyList());
					 concept.getTables().forEach(t -> t.setSelects(Collections.emptyList()));
				 });

        return new EntityDateQueryPlan(
                query.createQueryPlan(context),
                features.createQueryPlan(context),
                resolutionsAndAlignments,
                dateRange
        );
    }

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
		features.collectRequiredQueries(requiredQueries);
	}

    @Override
    public void resolve(QueryResolveContext context) {
        query.resolve(context.withDateAggregationMode(dateAggregationMode));
        features.resolve(context);
    }

    @Override
    public List<ResultInfo> getResultInfos() {
		List<ResultInfo>  resultInfos = new ArrayList<>();
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
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return query.collectRequiredEntities(context);
	}
}
