package com.bakdata.conquery.models.query;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQTemporal;
import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;

@Data
@AllArgsConstructor
@With
public class QueryExecutionContext {

	private final ManagedExecutionId executionId;
	private final QueryExecutor executor;
	private final ModificationShieldedWorkerStorage storage;
	private final BucketManager bucketManager;

	private int today = CDate.ofLocalDate(LocalDate.now());

	private ValidityDate validityDateColumn;

	private Connector connector;

	@NonNull
	private CDateSet dateRestriction = CDateSet.createFull();
	@NonNull
	private Optional<Aggregator<CDateSet>> queryDateAggregator = Optional.empty();


	private Map<CQTemporal, CDateSet> temporalQueryResult = new HashMap<>();

	/**
	 * Only set when in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the selected {@link SecondaryIdDescriptionId}.
	 */
	private SecondaryIdDescription activeSecondaryId = null;

	public QueryExecutionContext(ManagedExecutionId executionId, QueryExecutor executor, ModificationShieldedWorkerStorage storage, BucketManager bucketManager) {
		this.executionId = executionId;
		this.executor = executor;
		this.storage = storage;
		this.bucketManager = bucketManager;

	}

	public List<Bucket> getEntityBucketsForTable(Entity entity, Table table) {
		return bucketManager.getEntityBucketsForTable(entity, table);
	}

	boolean isQueryCancelled() {
		return executor.isCancelled(executionId);
	}
}