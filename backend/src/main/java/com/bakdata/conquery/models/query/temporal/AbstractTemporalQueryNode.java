package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.temporal.TemporalSampler;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import lombok.Getter;

import java.util.OptionalInt;
import java.util.Set;

@Getter
public abstract class AbstractTemporalQueryNode extends QPNode {

	private QueryPlan index;

	private QueryPlan preceding;

	private TemporalSampler sampler;

	private SpecialDateUnion dateUnion;

	public AbstractTemporalQueryNode(QueryPlan index, QueryPlan preceding, TemporalSampler sampler, SpecialDateUnion dateUnion) {
		this.index = index;
		this.preceding = preceding;
		this.sampler = sampler;
		this.dateUnion = dateUnion;
	}

	@Override
	public abstract QPNode clone(QueryPlan plan, QueryPlan clone);

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		index.getRoot().collectRequiredTables(out);
		preceding.getRoot().collectRequiredTables(out);
	}

	@Override
	public void init(Entity entity) {
		super.init(entity);

		index.getRoot().init(entity);
		preceding.getRoot().init(entity);
	}

	@Override
	public void nextBlock(Block block) {
		index.getRoot().nextBlock(block);
		preceding.getRoot().nextBlock(block);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		index.getRoot().nextTable(ctx, currentTable);
		preceding.getRoot().nextTable(ctx, currentTable);
	}

	@Override
	public boolean nextEvent(Block block, int event) {
		index.getRoot().aggregate(block, event);
		preceding.getRoot().aggregate(block, event);

		return true;
	}

	public void removePreceding(CDateSet preceding, int sample) {
		// Only consider samples that are before index's sample event
		preceding.remove(CDateRange.atLeast(sample));
	}

	@Override
	public final boolean isContained() {
		if (!(index.getRoot().isContained() && preceding.getRoot().isContained())) {
			return false;
		}

		CDateSet indexDurations = getIndex().getIncluded().getAggregationResult();
		// Create copy as we are mutating the set
		CDateSet precedingDurations = CDateSet.create(getPreceding().getIncluded().getAggregationResult());


		OptionalInt sampledIndex = getSampler().sample(indexDurations);

		if (!sampledIndex.isPresent())
			return false;

		removePreceding(precedingDurations, sampledIndex.orElse(-1));

		OptionalInt sampledPreceding = sampler.sample(precedingDurations);

		if (!precedingDurations.isEmpty() && isContained(sampledIndex, sampledPreceding)) {
			dateUnion.merge(precedingDurations);
			return true;
		}

		return false;
	}

	protected abstract boolean isContained(OptionalInt index, OptionalInt preceding);

}
