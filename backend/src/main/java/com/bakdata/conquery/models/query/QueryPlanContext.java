package com.bakdata.conquery.models.query;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQAbstractTemporalQuery;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;
import com.bakdata.conquery.models.worker.Worker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.With;

@Data
@AllArgsConstructor
@With
public class QueryPlanContext {

	public QueryPlanContext(Worker worker){
		this(worker, null, Collections.emptyMap());
	}

	@Getter(AccessLevel.NONE)
	private final Worker worker;

	private final CDateRange dateRestriction = CDateRange.all();

	/**
	 * Set if in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the query-active {@link SecondaryIdDescriptionId}.
	 */
	@Nullable
	private final SecondaryIdDescription selectedSecondaryId;

	/**
	 * Used in {@link com.bakdata.conquery.models.query.queryplan.TimeBasedQueryPlan} execution.
	 * {@link CQAbstractTemporalQuery} does not produce QueryPlan elements in createQueryPlan, instead delegating to pre-executed nodes.
	 */
	private final Map<CQAbstractTemporalQuery, TemporalQueryNode> temporalQueryNodes;


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
