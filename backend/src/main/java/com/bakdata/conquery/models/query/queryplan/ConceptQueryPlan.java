package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.EmptyBucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import com.codahale.metrics.Meter;
import com.codahale.metrics.SharedMetricRegistries;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@Slf4j
public class ConceptQueryPlan implements QueryPlan<SinglelineEntityResult> {

	private static final Meter INTERESTING_ENTITY = SharedMetricRegistries.getDefault().meter("queries.interest.entity.true");
	private static final Meter INTERESTING_BUCKET = SharedMetricRegistries.getDefault().meter("queries.interest.bucket.true");
	private static final Meter NOT_INTERESTING_ENTITY = SharedMetricRegistries.getDefault().meter("queries.interest.entity.false");
	private static final Meter NOT_INTERESTING_BUCKET = SharedMetricRegistries.getDefault().meter("queries.interest.bucket.false");


	public static final int VALIDITY_DATE_POSITION = 0;


	@ToString.Exclude
	@Getter
	private final ThreadLocal<Set<Table>> requiredTables = ThreadLocal.withInitial(this::collectRequiredTables);

	private QPNode child;

	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();

	@ToString.Exclude
	private Entity entity;

	private DateAggregator dateAggregator = new DateAggregator(DateAggregationAction.MERGE);

	public ConceptQueryPlan(boolean generateDateAggregator) {
		if (generateDateAggregator) {
			aggregators.add(dateAggregator);
		}
	}


	public void init(QueryExecutionContext ctx, Entity entity) {
		this.entity = entity;
		child.init(entity, ctx);
	}

	public boolean nextEvent(Bucket bucket, int event) {
		return getChild().acceptEvent(bucket, event);
	}

	protected SinglelineEntityResult createResult() {
		Object[] values = new Object[aggregators.size()];

		for (int i = 0; i < values.length; i++) {
			values[i] = aggregators.get(i).createAggregationResult();
		}

		return new SinglelineEntityResult(entity.getId(), values);
	}

	@Override
	public Optional<SinglelineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		// Only override if none has been set from a higher level
		ctx = QueryUtils.determineDateAggregatorForContext(ctx, this::getValidityDateAggregator);

		if (!isOfInterest(entity)) {
			return Optional.empty();
		}

		// Always do one go-round with ALL_IDS_TABLE.
		nextTable(ctx, ctx.getStorage().getDataset().getAllIdsTable());
		nextBlock(EmptyBucket.getInstance());
		nextEvent(EmptyBucket.getInstance(), 0);

		for (Table currentTable : requiredTables.get()) {

			if (Dataset.isAllIdsTable(currentTable)) {
				continue;
			}

			nextTable(ctx, currentTable);

			final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

			log.trace("Table[{}] has {} buckets for Entity[{}]", currentTable, tableBuckets, entity);

			for (Bucket bucket : tableBuckets) {

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
			return Optional.of(createResult());
		}
		return Optional.empty();
	}


	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		child.nextTable(ctx, currentTable);
	}

	public void nextBlock(Bucket bucket) {
		child.nextBlock(bucket);
	}

	public void registerAggregator(Aggregator<?> aggregator) {
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
		final boolean interesting = !getRequiredTables().get().isEmpty() && child.isOfInterest(entity);

		reportEntityInterest(interesting);

		return interesting;
	}

	private void reportEntityInterest(boolean interesting) {
		if (!log.isTraceEnabled()) {
			return;
		}

		if (interesting) {
			INTERESTING_ENTITY.mark();
		}
		else {
			NOT_INTERESTING_ENTITY.mark();
		}
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		if (!isAggregateValidityDates()) {
			// The date aggregator was not added to the plan, so we don't collect a validity date
			return Optional.empty();
		}

		return Optional.of(dateAggregator);
	}

	public boolean isAggregateValidityDates() {
		return dateAggregator.equals(aggregators.get(0));
	}

	public boolean isOfInterest(Bucket bucket) {
		final boolean interesting = bucket.containsEntity(entity.getId()) && child.isOfInterest(bucket);

		reportBucketInterest(interesting);

		return interesting;
	}

	private void reportBucketInterest(boolean interesting) {
		if (!log.isTraceEnabled()) {
			return;
		}

		if (interesting) {
			INTERESTING_BUCKET.mark();
		}
		else {
			NOT_INTERESTING_BUCKET.mark();
		}
	}

	public Set<Table> collectRequiredTables() {
		return child.collectRequiredTables();
	}

}