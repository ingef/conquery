package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ConstantValueAggregator;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.FixedLabelResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Iterators;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Temporal queries allow users to query for temporal relationships:
 *
 * <pre>
 * {
 * 	"type" : "TEMPORAL",
 * 	"reference" : {}, // Concept A
 * 	"compare" : {}, // Concept B
 * 	"mode" : {"type": "BEFORE", "days" : {"min" : 10}},
 * 	"referenceSelector" : "EARLIEST",
 * 	"compareSelector" : "EARLIEST",
 * }
 * </pre>
 * => The earliest sub-period of Concept A is included, if the earliest sub-period of B is at least 10 days before it.
 * <p>
 * <hr />
 *
 * <pre>
 * {
 * 	"type" : "TEMPORAL",
 * 	"reference" : {}, // Concept A
 * 	"compare" : {}, // Concept B
 * 	"mode" : {"type": "BEFORE", "days" : {"min" : 10}},
 * 	"referenceSelector" : "ANY",
 * 	"compareSelector" : "EARLIEST",
 * }
 * </pre>
 * => All sub-periods of Concept A are included, if the earliest sub-period of Concept B is 10 days before it.
 * <p>
 * <hr />
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
	private final boolean showCompareDate;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final ConceptQueryPlan indexSubPlan = createIndexPlan(getIndexQuery(), context, plan);

		ConstantValueAggregator<CDateSet> compareDateAggregator = new ConstantValueAggregator<>(null, new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE));
		if (showCompareDate){
			plan.registerAggregator(compareDateAggregator);
		}

		// These aggregators will be fed with the actual aggregation results of the sub results
		final List<ConstantValueAggregator<List>> shimAggregators = createShimAggregators();

		shimAggregators.forEach(plan::registerAggregator);

		final TemporalSubQueryPlan subQuery =
				new TemporalSubQueryPlan(getIndexSelector(), getMode(), getCompareSelector(), getCompareQuery(), context, indexSubPlan,
										 shimAggregators.stream().map(ConstantValueAggregator::getValue).collect(Collectors.toList()),
										 compareDateAggregator
				);

		return new TimeBasedQueryNode(context.getStorage().getDataset().getAllIdsTable(), subQuery);
	}


	private ConceptQueryPlan createIndexPlan(CQElement query, QueryPlanContext context, ConceptQueryPlan plan) {
		final ConceptQueryPlan subPlan = new ConceptQueryPlan(true);
		final QPNode indexNode = query.createQueryPlan(context, plan);

		subPlan.getDateAggregator().registerAll(indexNode.getDateAggregators());
		subPlan.setChild(indexNode);
		return subPlan;
	}

	/**
	 * Makes it such that Before is just a special case of After, reducing code and testing requirements.
	 */
	private CQElement getIndexQuery() {
		return switch (mode) {
			case Mode.Before ignored -> compare;

			case Mode.After ignored -> index;
			case Mode.While ignored -> index;
		};
	}

	private List<ConstantValueAggregator<List>> createShimAggregators() {
		//TODO Don't create list, if it's not ALL?
		return compare.getResultInfos()
					  .stream()
					  .map(info -> new ConstantValueAggregator<List>(new ArrayList<>(), new ResultType.ListT(info.getType())))
					  .toList();
	}

	/**
	 * Makes it such that Before is just a special case of After, reducing code and testing requirements.
	 */
	private CQElement getCompareQuery() {
		return switch (mode) {
			case Mode.Before ignored -> index;

			case Mode.After ignored -> compare;
			case Mode.While ignored -> compare;
		};
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		index.collectRequiredQueries(requiredQueries);
		compare.collectRequiredQueries(requiredQueries);
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

		if (showCompareDate) {
			resultInfos.add(new FixedLabelResultInfo(new ResultType.ListT<>(ResultType.Primitive.DATE_RANGE), Collections.emptySet()) {
				@Override
				public String userColumnName(PrintSettings printSettings) {
					//TODO localized label
					return index.userLabel(printSettings.getLocale()) + " - Compare Dates";
				}
			});
		}

		resultInfos.addAll(compare.getResultInfos());
		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().collectRequiredEntities(context);
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
		},
		ALL {
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
		},
		EARLIEST {
			@Override
			public CDateRange[] sample(CDateSet result) {
				return new CDateRange[]{result.asRanges().iterator().next()};
			}

			@Override
			public boolean satisfies(boolean[] results) {
				return results[0];
			}
		},
		LATEST {
			@Override
			public CDateRange[] sample(CDateSet result) {
				if (result.isEmpty()) {
					return new CDateRange[0];
				}


				final Iterator<CDateRange> iterator = result.asRanges().iterator();

				return new CDateRange[]{Iterators.getLast(iterator)};
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
	public sealed interface Mode permits Mode.After, Mode.While {

		CDateRange[] convert(CDateRange[] in, Selector selector);

		@CPSType(id = "BEFORE", base = Mode.class)
		final
		class Before extends After {

			@JsonCreator
			public Before(Range.IntegerRange days) {
				super(days);
			}


		}

		@CPSType(id = "AFTER", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		sealed class After implements Mode permits Mode.Before {

			@NotNull
			private final Range.IntegerRange days;

			public CDateRange[] convert(CDateRange[] parts, Selector selector) {

				if (parts.length == 0){
					return  new CDateRange[0];
				}


				final OptionalInt maybeIndexDay = switch (selector) {
					case EARLIEST, ANY -> {
						if (!parts[0].hasLowerBound()) {
							yield OptionalInt.empty();
						}

						yield Arrays.stream(parts)
									.mapToInt(CDateRange::getMinValue)
									.min();
					}

					case LATEST, ALL -> {
						if (!parts[parts.length - 1].hasUpperBound()){
							yield OptionalInt.empty();
						}

						yield Arrays.stream(parts)
									.mapToInt(CDateRange::getMaxValue)
									.max();
					}
				};

				if (maybeIndexDay.isEmpty()) {
					return new CDateRange[0];
				}

				int indexDay = maybeIndexDay.getAsInt();

				if (days == null || days.isAll()) {
					return new CDateRange[]{CDateRange.atLeast(indexDay)};
				}

				if (days.isOpen()) {
					if (days.isAtLeast()) {
						return new CDateRange[]{CDateRange.atLeast(indexDay + days.getMin())};
					}

					if (days.isAtMost()) {
						return new CDateRange[]{CDateRange.of(indexDay, indexDay + days.getMax())};
					}
				}

				return new CDateRange[]{CDateRange.of(indexDay + days.getMin(), indexDay + days.getMax())};

			}

		}

		@CPSType(id = "WHILE", base = Mode.class)
		@Data
		final class While implements Mode {

			public CDateRange[] convert(CDateRange[] in, Selector selector) {
				return in;
			}
		}

	}
}
