package com.bakdata.conquery.models.query.queryplan.specific.temporal;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQTemporal;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.DateRestrictingNode;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.ResultType;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class TemporalSubQueryPlan implements QueryPlan<EntityResult> {

	private final CQTemporal.Selector selector;
	private final CQTemporal.Mode mode;

	private final QPNode before;

	private final QPNode after;

	private final CQTemporal ref;

	private ConceptQueryPlan beforePlan;

	private CDateSet result;


	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		beforePlan = new ConceptQueryPlan(true);

		beforePlan.setChild(before);
		beforePlan.getDateAggregator().registerAll(before.getDateAggregators());

		beforePlan.init(ctx, entity);

		result = CDateSet.createEmpty();
	}

	@Override
	public Optional execute(QueryExecutionContext ctx, Entity entity) {


		final Optional<SinglelineEntityResult> subResult = beforePlan.execute(ctx, entity);

		if (subResult.isEmpty()){
			return Optional.empty();
		}

		final List<CDateRange> partitions = selector.sample(beforePlan.getDateAggregator().createAggregationResult());

		final BooleanList results = new BooleanArrayList(partitions.size());

		for (CDateRange partition : partitions) {

			final ConceptQueryPlan cqp = new ConceptQueryPlan(true);

			cqp.setChild(new DateRestrictingNode(CDateSet.create(mode.convert(partition)), after));

			cqp.getDateAggregator().registerAll(after.getDateAggregators());

			cqp.init(ctx, entity);

			final Optional<SinglelineEntityResult> entityResult = cqp.execute(ctx, entity);

			results.add(entityResult.isPresent());

			if (entityResult.isPresent()) {
				result.add(partition);
			}
		}

		final boolean satisfies = selector.satisfies(results);

		if (!satisfies) {
			return Optional.empty();
		}

		return Optional.of(new SinglelineEntityResult(entity.getId(), null));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return before.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		return Optional.of(new ConstantValueAggregator<>(result, new ResultType.ListT(ResultType.DateT.INSTANCE)));
	}
}
