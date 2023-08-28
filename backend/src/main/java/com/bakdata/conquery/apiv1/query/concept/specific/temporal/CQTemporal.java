package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

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
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Abstract data class specifying the data needed for a TemporalQuery.
 */
@Data
@CPSType(id = "TEMPORAL", base = CQElement.class)
public class CQTemporal extends CQElement {

	private final CQElement index;
	private final Mode mode;
	private final Selector selector;

	private final CQElement preceding;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		final QPNode indexPlan = index.createQueryPlan(context, plan);
		final QPNode precedingPlan = preceding.createQueryPlan(context, plan);

		final TemporalSubQueryPlan subQuery = new TemporalSubQueryPlan(getSelector(), getMode(), indexPlan, precedingPlan);

		return new TimeBasedQueryNode(context.getStorage().getDataset().getAllIdsTable(), subQuery);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		index.visit(visitor);
		preceding.visit(visitor);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		index.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
		preceding.resolve(context.withDateAggregationMode(DateAggregationMode.MERGE));
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		final List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getResultInfos());
		resultInfos.addAll(preceding.getResultInfos());
		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().collectRequiredEntities(context); //TODO preceeding also?
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "mode")
	@CPSBase
	public interface Mode {
		CDateRange convert(CDateRange in);
		@CPSType(id = "BEFORE", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		class Before implements Mode {

			private final Range.IntegerRange days;

			public CDateRange convert(CDateRange in) {
				if (!in.hasLowerBound()) {
					return null;
				}

				if(days == null){
					return in;
				}

				final int min = in.getMinValue();

				if (!days.isOpen()) {
					return CDateRange.of(min - days.getMax(), min - days.getMin());
				}

				if (days.hasLowerBound()) {
					return CDateRange.atMost(min - days.getMin());
				}

				if (days.hasUpperBound()) {
					return CDateRange.atLeast(min - days.getMax());
				}

				return in; // => days.isAll
			}
		}

		@CPSType(id = "AFTER", base = Mode.class)
		@Data
		@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
		class After implements Mode {

			private final Range.IntegerRange days;

			public CDateRange convert(CDateRange in) {
				if (!in.hasUpperBound()) {
					return null;
				}

				if(days == null){
					return in;
				}

				final int max = in.getMaxValue();

				if (!days.isOpen()) {
					return CDateRange.of(max + days.getMin(), max + days.getMax());
				}

				if (days.hasLowerBound()) {
					return CDateRange.atLeast(max + days.getMin());
				}

				if (days.hasUpperBound()) {
					return CDateRange.atMost(max + days.getMax());
				}

				return in; // => days.isAll
			}

		}
		@CPSType(id = "WHILE", base = Mode.class)
		@Data
		class While implements Mode {

			public CDateRange convert(CDateRange in) {
				return in;
			}
		}

	}


	public enum Selector {
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
				if (result.isEmpty()) {
					return Collections.emptyList();
				}

				final Iterator<CDateRange> iterator = result.asRanges().iterator();
				CDateRange last = null;

				while (iterator.hasNext()) {
					last = iterator.next();
				}

				return List.of(last);
			}

			@Override
			public boolean satisfies(BooleanList results) {
				return results.getBoolean(0);
			}
		};

		public abstract List<CDateRange> sample(CDateSet result);

		public abstract boolean satisfies(BooleanList results);
	}
}
