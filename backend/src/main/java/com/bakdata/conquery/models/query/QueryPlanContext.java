package com.bakdata.conquery.models.query;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.worker.Worker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor @AllArgsConstructor @Getter @With
public class QueryPlanContext {

	@Getter(AccessLevel.NONE)
	private final Worker worker;

	//TODO this is not used, delete it?
	private CDateSet dateRestriction = CDateSet.createFull();


	/**
	 * Set if in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the query-active {@link SecondaryIdDescriptionId}.
	 */
	@Nullable
	private SecondaryIdDescription selectedSecondaryId;

	public Dataset getDataset() {
		return worker.getStorage().getDataset();
	}

	public ModificationShieldedWorkerStorage getStorage() {
		return worker.getStorage();
	}

	public CentralRegistry getCentralRegistry() {
		return worker.getStorage().getCentralRegistry();
	}

}
