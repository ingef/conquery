package com.bakdata.conquery.models.forms.managed;

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
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@CPSType(id = "ENTITY_DATE_QUERY", base = QueryDescription.class)
@Getter
@RequiredArgsConstructor
public class EntityDateQuery extends IQuery {

    @NotNull
    @Valid
    private final IQuery query;
    @NotNull @Valid
    private final ArrayConceptQuery features;

    @NotNull @NotEmpty
    private final List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments;

    @NotNull @Valid
    private final CDateRange dateRange;



    @Override
    public EntityDateQueryPlan createQueryPlan(QueryPlanContext context) {
        // Clear all selects we need only the date union which is enforced through the content
        query.visit(v -> {
            if(v instanceof CQConcept) {
                CQConcept concept = ((CQConcept) v);
                concept.setSelects(Collections.emptyList());
                concept.getTables().forEach(t -> t.setSelects(Collections.emptyList()));
            }
        });
        return new EntityDateQueryPlan(
                query.createQueryPlan(context.withGenerateSpecialDateUnion(true)),
                features.createQueryPlan(context.withGenerateSpecialDateUnion(true)),
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
        query.resolve(context);
        features.resolve(context);
    }

    @Override
    public void collectResultInfos(ResultInfoCollector collector) {
        features.collectResultInfos(collector);
        //remove SpecialDateUnion
        //collector.getInfos().remove(0);

        collector.getInfos().add(0, ConqueryConstants.RESOLUTION_INFO);
        collector.getInfos().add(1, ConqueryConstants.CONTEXT_INDEX_INFO);
        collector.getInfos().add(2, ConqueryConstants.DATE_RANGE_INFO);
    }

    @Override
    public void visit(Consumer<Visitable> visitor) {
        query.visit(visitor);
        features.visit(visitor);
    }
}
