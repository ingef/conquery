package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.ArrayConceptQuery;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id = "ENTITY_DATE", base = Mode.class)
public class EntityDateMode extends Mode {

    @NotNull
    @Valid
    private Range<LocalDate> dateRange;

    @NotEmpty
    private List<CQElement> features = ImmutableList.of();

    @Override
    public void visit(Consumer<Visitable> visitor) {
        features.forEach(e -> visitor.accept(e));
    }

    @NotNull
    private DateContext.Alignment alignmentHint = DateContext.Alignment.QUARTER;

    @Override
    public void resolve(QueryResolveContext context) {
        // Resolve all
        features.forEach(e -> e.resolve(context));
    }

    @Override
    public IQuery createSpecializedQuery(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {
        // Apply defaults to user concept
        ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, datasets);
        return new EntityDateQuery(
				getForm().getPrerequisite(),
				ArrayConceptQuery.createFromFeatures(features),
				ExportForm.getResolutionAlignmentMap(getForm().getResolvedResolutions(), getAlignmentHint()),
				CDateRange.of(dateRange)
        );
    }
}
