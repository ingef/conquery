package com.bakdata.conquery.apiv1.forms.export_form;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.export.AbsExportGenerator;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(id="ABSOLUTE", base=Mode.class)
public class AbsoluteMode extends Mode {
	@NotNull @Valid
	private Range<LocalDate> dateRange;

	@NotEmpty
	private List<CQElement> features = ImmutableList.of();

	@Override
	public void visit(Consumer<Visitable> visitor) {
		features.forEach(visitor::accept);
	}

	@NotNull
	private DateContext.Alignment alignmentHint = DateContext.Alignment.QUARTER;

	@Override
	public Query createSpecializedQuery(DatasetRegistry datasets, User user, Dataset submittedDataset) {
		return AbsExportGenerator.generate(this);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		// Resolve all
		features.forEach(e -> e.resolve(context.withDateAggregationMode(DateAggregationMode.NONE)));
	}
}
