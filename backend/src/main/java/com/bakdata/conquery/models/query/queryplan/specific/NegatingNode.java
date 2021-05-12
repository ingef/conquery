package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class NegatingNode extends QPChainNode {

	private final DateAggregator dateAggregator;

	public NegatingNode(@NonNull QPNode child, @NonNull DateAggregationAction dateAction) {
		super(child);
		this.dateAggregator = new DateAggregator(dateAction);
		dateAggregator.register(child.getDateAggregators());
	}

	private NegatingNode(@NonNull QPNode child, @NonNull DateAggregator dateAggregator) {
		super(child);
		this.dateAggregator = dateAggregator;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}

	// Discard this for now as is all always seems to bubble up true
//	@Override
//	public boolean isOfInterest(Entity entity) {
//		return !super.isOfInterest(entity);
//	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}

	@Override
	public NegatingNode doClone(CloneContext ctx) {
		return new NegatingNode(ctx.clone(getChild()), ctx.clone(this.dateAggregator));
	}

	@Override
	public Optional<Boolean> eventFiltersApply(Bucket bucket, int event) {
		// Negate if present
		final Optional<Boolean> result = getChild().eventFiltersApply(bucket, event);
		return result.isPresent()? Optional.of(!result.get()) : result;
	}

	@Override
	public Optional<Boolean> aggregationFiltersApply() {
		final Optional<Boolean> aBoolean = getChild().aggregationFiltersApply();
		return aBoolean.isPresent()?Optional.of(!aBoolean.get()): aBoolean;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		if (dateAggregator != null) {
			return Set.of(dateAggregator);
		}
		return Collections.emptySet();
	}
}