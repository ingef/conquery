package com.bakdata.conquery.models.query;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.worker.Worker;
import lombok.*;

import javax.annotation.Nullable;

@RequiredArgsConstructor @AllArgsConstructor @Getter @With
public class QueryPlanContext {

	@Getter(AccessLevel.NONE)
	private final Worker worker;

	private CDateRange dateRestriction = CDateRange.all();


	/**
	 * Set if in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the query-active {@link SecondaryIdDescriptionId}.
	 */
	@Nullable
	private SecondaryIdDescriptionId selectedSecondaryId;

	public DatasetId getDataset() {
		return worker.getStorage().getDataset().getId();
	}

	public ModificationShieldedWorkerStorage getStorage() {
		return worker.getStorage();
	}

	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

	public BucketManager getBlockManager() {
		return worker.getBucketManager();
	}
}
