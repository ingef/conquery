package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.io.storage.ModificationShieldedWorkerStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.EmptyBucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@Slf4j
public class ConceptQueryPlan implements QueryPlan<SinglelineEntityResult> {

	private ThreadLocal<Set<Table>> requiredTables = new ThreadLocal<>();
	private QPNode child;
	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();
	private Entity entity;
	private DateAggregator dateAggregator = new DateAggregator(DateAggregationAction.MERGE);
	private List<ConceptQueryPlan> subQueries = new ArrayList<>();
	private boolean accepted = false;

	public ConceptQueryPlan(boolean generateDateAggregator) {
		if (generateDateAggregator){
			aggregators.add(dateAggregator);
		}
	}

	public void addSubquery(ConceptQueryPlan subplan) {
		subQueries.add(0, subplan);
	}

	@Override
	public ConceptQueryPlan doClone(CloneContext ctx) {
		checkRequiredTables(ctx.getStorage());

		// We set the date aggregator if needed by manually in the following for loop
		ConceptQueryPlan clone = new ConceptQueryPlan(false);
		clone.setChild(ctx.clone(child));

		for (Aggregator<?> agg : aggregators) {
			clone.aggregators.add(ctx.clone(agg));
		}

		clone.dateAggregator = ctx.clone(dateAggregator);
		clone.setRequiredTables(this.getRequiredTables());
		for (ConceptQueryPlan subQuery : this.subQueries) {
			clone.addSubquery(ctx.clone(subQuery));
		}
		return clone;
	}

	protected void checkRequiredTables(ModificationShieldedWorkerStorage storage) {
		if (requiredTables.get() != null) {
			return;
		}


		requiredTables.set(this.collectRequiredTables());

		// TODO This checks nothing
		// Assert that all tables are actually present
		for (Table table : requiredTables.get()) {
			if (Dataset.isAllIdsTable(table)) {
				continue;
			}
		}
	}

	public void init(Entity entity, QueryExecutionContext ctx) {
		this.entity = entity;
		child.init(entity, ctx);
	}

	public boolean nextEvent(Bucket bucket, int event) {
		final QPNode child = getChild();
		final boolean emptyBucket = EmptyBucket.getInstance().equals(bucket);
		final boolean accepted = emptyBucket || child.eventFiltersApply(bucket, event).orElse(true);
		if(accepted) {
			child.acceptEvent(bucket, event);
		}
		return accepted;
	}

	protected SinglelineEntityResult result() {
		Object[] values = new Object[aggregators.size()];

		for (int i = 0; i < values.length; i++) {
			values[i] = aggregators.get(i).getAggregationResult();
		}

		return new SinglelineEntityResult(entity.getId(), values);
	}

	@Override
	public Optional<SinglelineEntityResult> execute(final QueryExecutionContext ctx, Entity entity) {

		subQueries.forEach(sub -> sub.execute(ctx, entity));

		// Only override if none has been set from a higher level
		QueryExecutionContext resolvedCtx = QueryUtils.determineDateAggregatorForContext(ctx, this::getValidityDateAggregator);

 		checkRequiredTables(resolvedCtx.getStorage());

		if (requiredTables.get().isEmpty()) {
			return Optional.empty();
		}

		init(entity, resolvedCtx);

		if(!isOfInterest(entity)){
			return Optional.empty();
		}

		// Always do one go-round with ALL_IDS_TABLE.
		final Table allIdsTable = resolvedCtx.getStorage().getDataset().getAllIdsTable();
		if (requiredTables.get().contains(allIdsTable)) {
			nextTable(resolvedCtx, allIdsTable);
			nextBlock(EmptyBucket.getInstance());
			nextEvent(EmptyBucket.getInstance(), 0);
			accepted = true;
		}
		for (Table currentTable : requiredTables.get()) {

			if(Dataset.isAllIdsTable(currentTable)){
				continue;
			}

			nextTable(resolvedCtx, currentTable);

			final List<Bucket> tableBuckets = resolvedCtx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

			log.trace("Table[{}] has {} buckets for Entity[{}]", currentTable, tableBuckets, entity);

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
					accepted |= nextEvent(bucket, event);
				}
			}
		}

		if (isContained()) {
			return Optional.of(result());
		}
		log.warn("entity {} not contained", entity.getId());
		return Optional.empty();
	}

	public boolean isContained() {
		return accepted && aggregationFiltersApply().orElse(true);
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

	public Optional<Boolean> aggregationFiltersApply() {
		return child.aggregationFiltersApply();
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return child.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		if(!isAggregateValidityDates()) {
			// The date aggregator was not added to the plan, so we don't collect a validity date
			return Optional.empty();
		}

		return Optional.of(dateAggregator);
	}

	public boolean isAggregateValidityDates() {
		return (!aggregators.isEmpty()) && dateAggregator.equals(aggregators.get(0));
	}

	public boolean isOfInterest(Bucket bucket) {
		return child.isOfInterest(bucket);
	}

	public Set<Table> collectRequiredTables() {
		return child.collectRequiredTables();
	}

}