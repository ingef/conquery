package com.bakdata.conquery.models.query;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.storage.WorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data @With
@AllArgsConstructor @RequiredArgsConstructor
public class QueryPlanContext {

	private final WorkerStorage storage;
	private final int secondaryIdSubPlanRetention;

	private CDateRange dateRestriction = CDateRange.all();

	private boolean disableAggregators = false;
	private boolean disableAggregationFilters = true;


	/**
	 * Set if in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the query-active {@link SecondaryIdDescriptionId}.
	 */
	@Nullable
	private SecondaryIdDescription selectedSecondaryId;

	public Dataset getDataset() {
		return getStorage().getDataset();
	}


}
