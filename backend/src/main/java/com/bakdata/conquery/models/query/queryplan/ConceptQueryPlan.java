package com.bakdata.conquery.models.query.queryplan;

import java.util.*;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.EmptyBucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@Slf4j
public class ConceptQueryPlan implements QueryPlan {

	@Getter
	@Setter
	private ThreadLocal<Set<TableId>> requiredTables = new ThreadLocal<>();
	private QPNode child;
	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();
	private Entity entity;
	private DateAggregator dateAggregator = new DateAggregator(DateAggregationAction.PASS);

	public ConceptQueryPlan(DateAggregationMode dateAggregationMode) {
		if (!Objects.equals(dateAggregationMode,DateAggregationMode.NONE)){
			aggregators.add(dateAggregator);
		}
	}

	public ConceptQueryPlan(QueryPlanContext ctx) {
		this(ctx.getDateAggregationMode());
	}

	@Override
	public ConceptQueryPlan clone(CloneContext ctx) {
		checkRequiredTables(ctx.getStorage());

		ConceptQueryPlan clone = new ConceptQueryPlan(DateAggregationMode.NONE);
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
	public SinglelineEntityResult execute(QueryExecutionContext ctx, Entity entity) {

 		checkRequiredTables(ctx.getStorage());

		if (requiredTables.get().isEmpty()) {
			return EntityResult.notContained();
		}

		init(entity, ctx);

		if(!isOfInterest(entity)){
			return EntityResult.notContained();
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
			return result();
		}
		return EntityResult.notContained();
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

	public boolean isOfInterest(Bucket bucket) {
		return child.isOfInterest(bucket);
	}

	public Set<TableId> collectRequiredTables() {
		return child.collectRequiredTables();
	}

	public enum DateAggregationMode {
		NONE,
		MERGE,
		INTERSECT,
		LOGICAL;
	}


	public enum DateAggregationAction {
		BLOCK() {
			@Override
			public Set<CDateRange> aggregate(Set<CDateRange> all) {
				return Collections.emptySet();
			}
		},
		PASS(){
			@Override
			public Set<CDateRange> aggregate(Set<CDateRange> all) {
				return all;
			}
		},
		MERGE(){
			@Override
			public Set<CDateRange> aggregate(Set<CDateRange> all) {
				return CDateSet.create(all).asRanges();
			}
		},
		INTERSECT(){
			@Override
			public Set<CDateRange> aggregate(Set<CDateRange> all) {
				if(all.size() < 1) {
					return Collections.emptySet();
				}

				Iterator<CDateRange> it = all.iterator();
				CDateRange first = it.next();

				if(all.size() == 1) {
					return Set.of(first);
				}
				// Use the first range as mask and subtract all other ranges from it

				CDateSet intersection = CDateSet.create(first);

				// Intersect
				while(it.hasNext()){
					intersection.retainAll(it.next());
				}
				return intersection.asRanges();
			}
		},
		NEGATE() {
			@Override
			public Set<CDateRange> aggregate(Set<CDateRange> all) {
				CDateSet negative = CDateSet.createFull();
				negative.removeAll(all);
				return negative.asRanges();
			}
		};

		public abstract Set<CDateRange> aggregate(Set<CDateRange> all);
	}
}