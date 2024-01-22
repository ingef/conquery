package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.CheckForNull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id = "ENTITY_DATE", base = Mode.class)
public class EntityDateMode extends Mode {

    @CheckForNull
    @Valid
    private Range<LocalDate> dateRange;

    @NotNull
	private DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;


	@Override
	public void visit(Consumer<Visitable> visitor) {
	}

	@NotNull
	private Alignment alignmentHint = Alignment.QUARTER;


	@JsonView(View.InternalCommunication.class)
	@EqualsAndHashCode.Exclude
	private ArrayConceptQuery resolvedFeatures;

	@Override
	public void resolve(QueryResolveContext context) {
		resolvedFeatures = ArrayConceptQuery.createFromFeatures(getForm().getFeatures());
		resolvedFeatures.resolve(context);
    }

	@Override
	public Query createSpecializedQuery() {
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
