package com.bakdata.conquery.models.query;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import groovy.lang.Tuple3;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@With
public class QueryExecutionContext {

	private final ManagedExecutionId executionId;

	private final QueryExecutor executor;
	private final ModificationShieldedWorkerStorage storage;
	private final BucketManager bucketManager;


	private ValidityDate validityDateColumn;
	@NonNull
	private CDateSet dateRestriction = CDateSet.createFull();
	private Connector connector;
	@NonNull
	private Optional<Aggregator<CDateSet>> queryDateAggregator = Optional.empty();

	@Getter(AccessLevel.NONE)
	private final Map<Tuple3<Column, Import, String[]>, int[]> multiSelectValuesCache = new ConcurrentHashMap<>();


	private static int[] findIds(Column column, Bucket bucket, String[] values) {
		final int[] selectedValues = new int[values.length];

		final StringStore type = (StringStore) bucket.getStore(column);

		for (int index = 0; index < values.length; index++) {
			final String select = values[index];
			final int parsed = type.getId(select);

			selectedValues[index] = parsed;
		}

		return selectedValues;
	}

	public int[] getIdsFor(Column column, Bucket bucket, String[] values) {
		return multiSelectValuesCache.computeIfAbsent(new Tuple3<>(column, bucket.getImp(), values), (ignored) -> findIds(column, bucket, values));
	}

	/**
	 * Only set when in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the selected {@link SecondaryIdDescriptionId}.
	 */
	private SecondaryIdDescription activeSecondaryId = null;

	private final int today = CDate.ofLocalDate(LocalDate.now());

	public List<Bucket> getEntityBucketsForTable(Entity entity, Table table) {
		return bucketManager.getEntityBucketsForTable(entity, table);
	}

	boolean isQueryCancelled() {
		return executor.isCancelled(executionId);
	}

}