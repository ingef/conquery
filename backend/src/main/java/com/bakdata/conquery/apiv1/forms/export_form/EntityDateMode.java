package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id = "ENTITY_DATE", base = Mode.class)
public class EntityDateMode extends Mode {

    @CheckForNull
    @Valid
    private Range<LocalDate> dateRange;

    @NotEmpty
    private List<CQElement> features = ImmutableList.of();

    @NotNull
    private DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;

    @InternalOnly
    private ArrayConceptQuery resolvedFeatures;

    @Override
    public void visit(Consumer<Visitable> visitor) {
        features.forEach(e -> visitor.accept(e));
    }

    @NotNull
    private DateContext.Alignment alignmentHint = DateContext.Alignment.QUARTER;

    @Override
    public void resolve(QueryResolveContext context) {
    	// Apply defaults to user concept
        ExportForm.DefaultSelectSettable.enable(features);
		resolvedFeatures = ArrayConceptQuery.createFromFeatures(features);
        resolvedFeatures.resolve(context);
    }

    @Override
    public Query createSpecializedQuery(DatasetRegistry datasets, User user, Dataset submittedDataset) {
        CDateRange dateRestriction = dateRange == null ? CDateRange.all() : CDateRange.of(dateRange);

        return new EntityDateQuery(
                getForm().getPrerequisite(),
                resolvedFeatures,
                ExportForm.getResolutionAlignmentMap(getForm().getResolvedResolutions(), getAlignmentHint()),
                dateRestriction,
                dateAggregationMode
        );
    }
}
