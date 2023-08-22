package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQAbstractTemporalQuery;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class TemporalSubQueryPlan implements QueryPlan<EntityResult> {

	public enum Mode {
		ANY {
			@Override
			public List<CDateRange> sample(CDateSet result) {
				return new ArrayList<>(result.asRanges());
			}

			@Override
			public boolean satisfies(BooleanList results) {
				return results.stream().anyMatch(b -> b);
			}
		},
		ALL {
			@Override
			public List<CDateRange> sample(CDateSet result) {
				return new ArrayList<>(result.asRanges());
			}

			@Override
			public boolean satisfies(BooleanList results) {
				return results.stream().allMatch(b -> b);
			}
		},
		EARLIEST {
			@Override
			public List<CDateRange> sample(CDateSet result) {
				return List.of(result.asRanges().iterator().next());
			}

			@Override
			public boolean satisfies(BooleanList results) {
				return results.getBoolean(0);
			}
		},
		LATEST {
			@Override
			public List<CDateRange> sample(CDateSet result) {
				final Iterator<CDateRange> iterator = result.asRanges().iterator();
				CDateRange last = null;

				while (iterator.hasNext()){
					last = iterator.next();
				}

				return List.of(last);
			}

			@Override
			public boolean satisfies(BooleanList results) {
				return results.getBoolean(0);
			}
		}
		;
		public abstract List<CDateRange> sample(CDateSet result);
		public abstract boolean satisfies(BooleanList results);
	}

	private final Mode mode;

	private final QPNode before;

	private final QPNode after;

	private final CQAbstractTemporalQuery ref;


	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		before.init(entity, ctxt);
	}

	@Override
	public Optional execute(QueryExecutionContext ctx, Entity entity) {
		final ConceptQueryPlan plan = new ConceptQueryPlan(true);
		plan.setChild(before);

		final Optional<SinglelineEntityResult> result = plan.execute(ctx, entity);

		if (result.isEmpty()){
			return Optional.empty();
		}

		final List<CDateRange> partitions = mode.sample(plan.getDateAggregator().createAggregationResult());

		final BooleanList results = new BooleanArrayList(partitions.size());

		for (CDateRange partition : partitions) {
			final ConceptQueryPlan cqp = new ConceptQueryPlan(true);
			cqp.setChild(new DateRestrictingNode(CDateSet.create(partition), after));

			cqp.init(ctx, entity);
			final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

			results.add(entityResult.isPresent());
		}

		final boolean satisfies = mode.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), new Object[]{plan.getDateAggregator().createAggregationResult()}));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return false;
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.empty();
	}
}
