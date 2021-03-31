package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.EmptyBucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Getter
@Setter
@ToString
@Slf4j
public class ConceptQueryPlan implements QueryPlan<SinglelineContainedEntityResult> {

	public static final int VALIDITY_DATE_POSITION = 0;
	@Getter
	@Setter
	private ThreadLocal<Set<TableId>> requiredTables = new ThreadLocal<>();
	private QPNode child;
	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();
	private Entity entity;
	private DateAggregator dateAggregator = new DateAggregator(DateAggregationAction.MERGE);

	public ConceptQueryPlan(boolean generateDateAggregator) {
		if (generateDateAggregator){
			aggregators.add(dateAggregator);
		}
	}

	@Override
	public ConceptQueryPlan clone(CloneContext ctx) {
		checkRequiredTables(ctx.getStorage());

		// We set the date aggregator if needed by manually in the following for loop
		ConceptQueryPlan clone = new ConceptQueryPlan(false);
		clone.setChild(ctx.clone(child));

		for (Aggregator<?> agg : aggregators) {
			clone.aggregators.add(ctx.clone(agg));
		}

		clone.dateAggregator = ctx.clone(dateAggregator);
		clone.setRequiredTables(this.getRequiredTables());
		return clone;
	}

	protected void checkRequiredTables(ModificationShieldedWorkerStorage storage) {
		if (requiredTables.get() != null) {
			return;
		}


		requiredTables.set(this.collectRequiredTables());

		// Assert that all tables are actually present
		for (TableId tableId : requiredTables.get()) {
			if (Dataset.isAllIdsTable(tableId)) {
				continue;
			}

			if(storage.getTable(tableId) == null){
				throw new IllegalStateException("Table is missing");
			}
		}
	}

	public void init(Entity entity, QueryExecutionContext ctx) {
		this.entity = entity;
		child.init(entity, ctx);
	}

	public void nextEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}

	protected SinglelineContainedEntityResult result() {
		Object[] values = new Object[aggregators.size()];

		for (int i = 0; i < values.length; i++) {
			values[i] = aggregators.get(i).getAggregationResult();
		}

		return EntityResult.of(entity.getId(), values);
	}

	@Override
	public Optional<SinglelineContainedEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

 		checkRequiredTables(ctx.getStorage());

		if (requiredTables.get().isEmpty()) {
			return Optional.empty();
		}

		init(entity, ctx);

		if(!isOfInterest(entity)){
			return Optional.empty();
		}

		// Always do one go-round with ALL_IDS_TABLE.
		nextTable(ctx, ctx.getStorage().getDataset().getAllIdsTableId());
		nextBlock(EmptyBucket.getInstance());
		nextEvent(EmptyBucket.getInstance(), 0);

		for (TableId currentTableId : requiredTables.get()) {

			if(currentTableId.equals(ctx.getStorage().getDataset().getAllIdsTableId())){
				continue;
			}

			nextTable(ctx, currentTableId);

			final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTableId);

			log.trace("Table[{}] has {} buckets for Entity[{}]", currentTableId, tableBuckets, entity);

			for (Bucket bucket : tableBuckets) {

				if(bucket == null){
					continue;
				}

				if (!bucket.containsEntity(entity.getId())) {
					continue;
				}

				if (!isOfInterest(bucket)) {
					continue;
				}

				nextBlock(bucket);
				int start = bucket.getEntityStart(entity.getId());
				int end = bucket.getEntityEnd(entity.getId());
				for (int event = start; event < end; event++) {
					nextEvent(bucket, event);
				}
			}
		}

		if (isContained()) {
			return Optional.of(result());
		}
		return Optional.empty();
	}

	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		child.nextTable(ctx, currentTable);
	}

	public void nextBlock(Bucket bucket) {
		child.nextBlock(bucket);
	}

	public void addAggregator(Aggregator<?> aggregator) {
		aggregators.add(aggregator);
	}

	public int getAggregatorSize() {
		return aggregators.size();
	}

	public boolean isContained() {
		return child.isContained();
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return child.isOfInterest(entity);
	}

	@Override
	public CDateSet getValidityDates(SinglelineContainedEntityResult result) {
		if(!isAggregateValidityDates()) {
			// The date aggregator was not added to the plan, so we don't collect a validity date
			return CDateSet.create();
		}

		Object dates = result.getValues()[VALIDITY_DATE_POSITION];
		if(dates == null) {
			return CDateSet.create();
		}
		return (CDateSet) dates;
	}

	public boolean isAggregateValidityDates() {
		return dateAggregator.equals(aggregators.get(0));
	}

	public boolean isOfInterest(Bucket bucket) {
		return child.isOfInterest(bucket);
	}

	public Set<TableId> collectRequiredTables() {
		return child.collectRequiredTables();
	}


}