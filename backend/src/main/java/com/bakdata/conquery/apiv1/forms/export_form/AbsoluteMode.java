package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "ABSOLUTE", base = Mode.class)
public class AbsoluteMode extends Mode {
	@NotNull
	@Valid
	private Range<LocalDate> dateRange;


	@NotNull
	private Alignment alignmentHint = Alignment.QUARTER;


	@JsonView(View.InternalCommunication.class)
	private ArrayConceptQuery resolvedFeatures;

	@Override
	public Query createSpecializedQuery(DatasetRegistry datasets, User user, Dataset submittedDataset) {

		List<ExportForm.ResolutionAndAlignment> resolutionsAndAlignments =
				ExportForm.getResolutionAlignmentMap(getForm().getResolvedResolutions(), getAlignmentHint());

		return new AbsoluteFormQuery(
				getForm().getPrerequisite(),
				dateRange,
				resolvedFeatures,
				resolutionsAndAlignments
		);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		resolvedFeatures = ArrayConceptQuery.createFromFeatures(getForm().getFeatures());

		// Resolve all
		resolvedFeatures.resolve(context.withDateAggregationMode(DateAggregationMode.NONE));
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}
}
