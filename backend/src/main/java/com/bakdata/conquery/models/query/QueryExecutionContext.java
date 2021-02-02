package com.bakdata.conquery.models.query;

import java.util.List;

import com.bakdata.conquery.io.xodus.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.BucketManager;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.entity.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Getter @AllArgsConstructor @RequiredArgsConstructor
@With
public class QueryExecutionContext {

	private Column validityDateColumn;
	@NonNull
	private CDateSet dateRestriction = CDateSet.createFull();
	private boolean prettyPrint = true;
	private Connector connector;
	private final ModificationShieldedWorkerStorage storage;
	private final BucketManager bucketManager;


	/**
	 * Only set when in {@link com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan}, to the selected {@link SecondaryIdDescriptionId}.
	 */
	private SecondaryIdDescriptionId activeSecondaryId = null;

	public List<Bucket> getEntityBucketsForTable(Entity entity, TableId id) {
		return bucketManager.getEntityBucketsForTable(entity, id);
	}
}