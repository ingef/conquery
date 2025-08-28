package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import c10n.C10N;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.internationalization.ResultHeadersC10n;
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
import lombok.Setter;

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

	@Setter
	private boolean showCompareDate;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {

		final ConceptQueryPlan indexSubPlan = createIndexPlan(getIndexQuery(), context, plan);

		// These aggregators will be fed with the actual aggregation results of the sub results
		final List<ConstantValueAggregator<List>> shimAggregators = createShimAggregators();


		final TemporalSubQueryPlan subQuery = new TemporalSubQueryPlan(getIndexSelector(),
																	   getMode(),
																	   getCompareSelector(),
																	   getCompareQuery(),
																	   context,
																	   indexSubPlan,
																	   shimAggregators.stream().map(ConstantValueAggregator::getValue).collect(Collectors.toList())
		);

		if (showCompareDate) {
			plan.registerAggregator(subQuery.getIndexDateAggregator());
		}

		shimAggregators.forEach(plan::registerAggregator);


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
		return compare.getResultInfos().stream().map(info -> new ConstantValueAggregator<List>(new ArrayList<>())).toList();
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
					return C10N.get(ResultHeadersC10n.class, printSettings.getLocale()).temporalCompareLabel(index.userLabel(printSettings.getLocale()));
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


	/**
	 * Defines constraints on the relation between index and compare
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@CPSBase
	sealed public interface Mode permits Mode.After, Mode.While {

		/**
		 * With selector,
		 */
		CDateRange[] convert(CDateRange[] in, Selector selector);

		/**
		 * Constraints compare to be after index-period.
		 * <br />
		 * days defines the span of days in which compare may happen:
		 * - days.min is the minimum days, compare needs to be after index
		 * - days.max is the maximum days, compare may be after index
		 * <br />
		 * e.g.
		 * - if index is 2010-01-01, days is {5/10}, then compare must be within {2010-01-06/2010-01-11}
		 * - if index is 2010-01-01, days is {5/+inf}, then compare must be within {2010-01-06/+inf}
		 * - if index is 2010-01-01, days is {+inf/10}, then compare must be within {2010-01-01/2010-01-11}
		 */
		@CPSType(id = "AFTER", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		sealed class After implements Mode permits Before {

			@NotNull
			private final Range.IntegerRange days;


			public CDateRange[] convert(CDateRange[] parts, Selector selector) {

				if (parts.length == 0) {
					return new CDateRange[0];
				}

				if (selector == Selector.EARLIEST) {
					CDateRange period = parts[0];
					if (!period.hasLowerBound()) {
						return new CDateRange[0];
					}

					return new CDateRange[]{applyDays(period.getMinValue())};
				}

				if (selector == Selector.LATEST) {
					CDateRange period = parts[parts.length - 1];
					if (!period.hasUpperBound()) {
						return new CDateRange[0];
					}

					return new CDateRange[]{applyDays(period.getMaxValue())};
				}

				CDateRange[] converted = new CDateRange[parts.length];

				for (int index = 0; index < parts.length; index++) {

					final OptionalInt maybeIndexDay = selectIndexDay(selector, parts[index]);

					if (maybeIndexDay.isEmpty()) {
						continue;
					}

					converted[index] = applyDays(maybeIndexDay.getAsInt());
				}

				return converted;
			}

			private CDateRange applyDays(int indexDay) {
				if (days == null || days.isAll()) {
					return CDateRange.atLeast(indexDay);
				}

				if (days.isOpen()) {
					if (days.isAtLeast()) {
						return CDateRange.atLeast(indexDay + days.getMin());


					}
					if (days.isAtMost()) {
						return CDateRange.of(indexDay, indexDay + days.getMax());
					}
				}
				return CDateRange.of(indexDay + days.getMin(), indexDay + days.getMax());

			}

			private OptionalInt selectIndexDay(Selector selector, CDateRange period) {
				return switch (selector) {
					case ANY -> {
						if (!period.hasLowerBound()) {
							yield OptionalInt.empty();
						}

						yield OptionalInt.of(period.getMinValue());
					}
					case ALL -> {
						if (!period.hasUpperBound()) {
							yield OptionalInt.empty();
						}

						yield OptionalInt.of(period.getMaxValue());
					}
					default -> throw new IllegalStateException("%s should already be handled.".formatted(selector));
				};
			}

		}

		/**
		 * Special case of AFTER, with compare and index flipped.
		 */
		@CPSType(id = "BEFORE", base = Mode.class)
		final class Before extends After {

			@JsonCreator
			public Before(Range.IntegerRange days) {
				super(days);
			}
		}


		/**
		 * Compare must happen within WHILE, this means, they must intersect.
		 */
		@CPSType(id = "WHILE", base = Mode.class)
		@Data
		final class While implements Mode {

			public CDateRange[] convert(CDateRange[] in, Selector selector) {
				if (in.length == 0) {
					return in;
				}
				return switch (selector) {
					case ANY, ALL -> in;
					case EARLIEST -> new CDateRange[]{in[0]};
					case LATEST -> new CDateRange[]{in[in.length - 1]};
				};
			}
		}

	}
}
