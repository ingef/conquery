package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Abstract data class specifying the data needed for a TemporalQuery.
 */
@Data
@CPSType(id = "TEMPORAL", base = CQElement.class)
public class CQTemporal extends CQElement {

	@Valid
	private final CQElement index;

	@Valid
	private final Mode mode;
	private final Selector indexSelector;

	private final Selector compareSelector;

	private final CQElement compare;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final ConceptQueryPlan indexSubPlan = createIndexPlan(context, plan);
		final List<ConstantValueAggregator<List>> shimAggregators = createShimAggregators();

		shimAggregators.forEach(plan::registerAggregator);

		final TemporalSubQueryPlan subQuery =
				new TemporalSubQueryPlan(getIndexSelector(), getMode(), getCompareSelector(), compare, context, indexSubPlan,
										 shimAggregators.stream().map(ConstantValueAggregator::getValue).collect(Collectors.toList())
				);

		return new TimeBasedQueryNode(context.getStorage().getDataset().getAllIdsTable(), subQuery);
	}

	private List<ConstantValueAggregator<List>> createShimAggregators() {
		return compare.getResultInfos()
					  .stream()
					  .map(info -> new ConstantValueAggregator<List>(new ArrayList<>(), new ResultType.ListT(info.getType())))
					  .toList();
	}

	private ConceptQueryPlan createIndexPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		final ConceptQueryPlan indexSubPlan = new ConceptQueryPlan(true);
		final QPNode indexNode = index.createQueryPlan(context, plan);

		indexSubPlan.getDateAggregator().registerAll(indexNode.getDateAggregators());
		indexSubPlan.setChild(indexNode);
		return indexSubPlan;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		index.visit(visitor);
		compare.visit(visitor);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		index.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		compare.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getResultInfos());
		resultInfos.addAll(compare.getResultInfos());
		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().collectRequiredEntities(context); //TODO preceeding also?
	}


	public enum Selector {
		ANY {
			@Override
			public CDateRange[] sample(CDateSet result) {
				return result.asRanges().toArray(CDateRange[]::new);
			}

			@Override
			public boolean satisfies(boolean[] results) {
				for (boolean value : results) {
					if (value) {
						return true;
					}
				}
				return false;
			}
		}, ALL {
			@Override
			public CDateRange[] sample(CDateSet result) {
				return result.asRanges().toArray(CDateRange[]::new);
			}

			@Override
			public boolean satisfies(boolean[] results) {
				for (boolean value : results) {
					if (!value) {
						return false;
					}
				}
				return true;
			}
		}, EARLIEST {
			@Override
			public CDateRange[] sample(CDateSet result) {
				return new CDateRange[]{result.asRanges().iterator().next()};
			}

			@Override
			public boolean satisfies(boolean[] results) {
				return results[0];
			}
		}, LATEST {
			@Override
			public CDateRange[] sample(CDateSet result) {
				if (result.isEmpty()) {
					return new CDateRange[0];
				}

				final Iterator<CDateRange> iterator = result.asRanges().iterator();
				CDateRange last = iterator.next();

				while (iterator.hasNext()) {
					last = iterator.next();
				}

				return new CDateRange[]{last};
			}

			@Override
			public boolean satisfies(boolean[] results) {
				return results[0];
			}
		};

		public abstract CDateRange[] sample(CDateSet result);

		public abstract boolean satisfies(boolean[] results);
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@CPSBase
	public interface Mode {
		CDateRange[] convert(CDateRange[] in, ToIntFunction<CDateRange> daySelector, Selector selector);

		@CPSType(id = "BEFORE", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		class Before implements Mode {

			@NotNull
			private final Range.IntegerRange days;

			public CDateRange[] convert(CDateRange[] parts, ToIntFunction<CDateRange> daySelector, Selector selector) {

				final Optional<CDateRange> maybeFirst;
				if (selector == Selector.ANY) {
					// Before ANY is equal to before the latest
					maybeFirst = Arrays.stream(parts).filter(CDateRange::hasLowerBound).max(Comparator.comparingInt(daySelector));
				}
				else {
					// The other cases are either strictly (ALL) or incidentally (EARLIEST, LATEST) the first value
					maybeFirst = Arrays.stream(parts).filter(CDateRange::hasLowerBound).min(Comparator.comparingInt(daySelector));
				}

				if (maybeFirst.isEmpty()) {
					return new CDateRange[0];
				}

				final CDateRange first = maybeFirst.get();
				final int min = daySelector.applyAsInt(first);

				if (days == null) {
					// return the first value
					return new CDateRange[]{first};
				}

				if (!days.isOpen()) {
					return new CDateRange[]{CDateRange.of(min - days.getMax(), min - days.getMin())};
				}

				if (days.hasLowerBound()) {
					return new CDateRange[]{CDateRange.atMost(min - days.getMin())};
				}

				if (days.hasUpperBound()) {
					return new CDateRange[]{CDateRange.atLeast(min - days.getMax())};
				}

				return new CDateRange[0]; // all
			}
		}

		@CPSType(id = "AFTER", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		class After implements Mode {

			@NotNull
			private final Range.IntegerRange days;

			public CDateRange[] convert(CDateRange[] parts, ToIntFunction<CDateRange> daySelector, Selector selector) {
				final Optional<CDateRange> maybeLast;
				if (selector == Selector.ANY) {
					// After ANY is equal to after the first
					maybeLast = Arrays.stream(parts).filter(CDateRange::hasLowerBound).min(Comparator.comparingInt(daySelector));
				}
				else {
					// The other cases are either strictly (ALL) or incidentally (EARLIEST, LATEST) the last value
					maybeLast = Arrays.stream(parts).filter(CDateRange::hasLowerBound).max(Comparator.comparingInt(daySelector));
				}

				if (maybeLast.isEmpty()) {
					return new CDateRange[0];
				}

				CDateRange last = maybeLast.get();

				if (days == null) {
					// return the first value
					return new CDateRange[]{last};
				}

				final int max = daySelector.applyAsInt(last);

				if (!days.isOpen()) {
					return new CDateRange[]{CDateRange.of(max + days.getMin(), max + days.getMax())};
				}

				if (days.hasLowerBound()) {
					return new CDateRange[]{CDateRange.atLeast(max + days.getMin())};
				}

				if (days.hasUpperBound()) {
					return new CDateRange[]{CDateRange.atMost(max + days.getMax())};
				}

				return new CDateRange[0]; // all
			}

		}

		@CPSType(id = "WHILE", base = Mode.class)
		@Data
		class While implements Mode {

			public CDateRange[] convert(CDateRange[] in, ToIntFunction<CDateRange> ignored, Selector selector) {
				return in;
			}
		}

	}
}
