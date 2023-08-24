package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import lombok.Data;

/**
 * Abstract data class specifying the data needed for a TemporalQuery.
 */
@Data
@CPSType(id = "TEMPORAL", base = CQElement.class)
public class CQTemporal extends CQElement {

	/**
	 * The query being executed as reference for preceding.
	 */
	private final CQElement index;
	private final Mode mode;
	private final Selector selector;
	/**
	 * The query being executed, compared to index. Events in preceding will be cut-off to be always before index, or at the same day, depending on the queries specific implementations.
	 */
	private final CQElement preceding;
	private QPNode preceedingPlan;
	private QPNode indexPlan;

	@Override
	public final QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		// Will not actually be evaluated here!
		indexPlan = index.createQueryPlan(context, plan);
		preceedingPlan = preceding.createQueryPlan(context, plan);

		return new TemporalRefNode(context.getStorage().getDataset().getAllIdsTable(), this);
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
		List<ResultInfo> resultInfos = new ArrayList<>();
		resultInfos.addAll(index.getResultInfos());
		resultInfos.addAll(preceding.getResultInfos());
		return resultInfos;
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getIndex().collectRequiredEntities(context); //TODO preceeding also?
	}

	public enum Mode {
		BEFORE {
			public CDateRange convert(CDateRange in) {
				if (!in.hasLowerBound()) {
					return null;
				}

				return CDateRange.atMost(in.getMinValue() - 1);
			}
		},
		AFTER {
			public CDateRange convert(CDateRange in) {
				if (!in.hasUpperBound()) {
					return null;
				}

				return CDateRange.atLeast(in.getMaxValue() + 1);
			}
		},
		WHILE {
			public CDateRange convert(CDateRange in) {
				return in;
			}
		};

		public abstract CDateRange convert(CDateRange in);
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
