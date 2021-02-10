package com.bakdata.conquery.apiv1.forms.export_form;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.forms.export.AbsExportGenerator;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.util.ConceptManipulator;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.function.Consumer;

@Getter @Setter
@CPSType(id = "ENTITY_DATE", base = Mode.class)
public class EntityDateMode extends Mode {


    @NotEmpty
    private List<CQElement> features = ImmutableList.of();

    @Override
    public void visit(Consumer<Visitable> visitor) {
        features.forEach(e -> visitor.accept(e));
    }

    @Override
    public void resolve(QueryResolveContext context) {
        // Resolve all
        features.forEach(e -> e.resolve(context));
    }

    @Override
    public IQuery createSpecializedQuery(DatasetRegistry datasets, UserId userId, DatasetId submittedDataset) {
        // Apply defaults to user concept
        ConceptManipulator.DEFAULT_SELECTS_WHEN_EMPTY.consume(features, datasets);
        return new EntityDateQuery(getForm().getPrerequisite(), AbsExportGenerator.createSubQuery(features));
    }
}
