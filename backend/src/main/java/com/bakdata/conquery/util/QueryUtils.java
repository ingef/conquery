package com.bakdata.conquery.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQAnd;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryUtils {

	/**
	 * Checks if the query requires to resolve external ids.
	 *
	 * @return True if a {@link CQExternal} is found.
	 */
	public static class ExternalIdChecker implements QueryVisitor {

		private final List<CQExternal> elements = new ArrayList<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQExternal) {
				elements.add((CQExternal) element);
			}
		}

		public boolean resolvesExternalIds() {
			return elements.size() > 0;
		}
	}

	/**
	 * Log the entire Query tree into Metrics
	 */
	public static class QueryMetricsReporter implements QueryVisitor {

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQElement) {
				SharedMetricRegistries.getDefault().counter(MetricRegistry.name("queries", element.getClass().getSimpleName(), "usage")).inc();
			}

			if (element instanceof CQConcept) {
				((CQConcept) element).getSelects()
									 .forEach(select -> SharedMetricRegistries.getDefault().counter(MetricRegistry.name(select.getClass(), "usage")).inc());

				for (CQTable table : ((CQConcept) element).getTables()) {
					table.getFilters().forEach(filter -> {
						SharedMetricRegistries.getDefault()
											  .counter(MetricRegistry.name("queries", "concept", "class", filter.getFilter().getClass().getSimpleName(), "usage"))
											  .inc();
						SharedMetricRegistries.getDefault()
											  .counter(MetricRegistry.name("query", "concept", "filter", filter.getFilter().getId().toStringWithoutDataset(), "usage"))
											  .inc();
					});

					table.getSelects().forEach(select -> {
						SharedMetricRegistries.getDefault()
											  .counter(MetricRegistry.name("queries", "concept","class", select.getClass().getSimpleName(), "usage"))
											  .inc();

						SharedMetricRegistries.getDefault()
											  .counter(MetricRegistry.name("queries", "concept", "select", select.getId().toString(), ".usage"))
											  .inc();
					});
				}
			}
		}
	}

	/**
	 * Find first and only directly ReusedQuery in the queries tree, and return its
	 * Id. ie.: arbirtary CQAnd/CQOr with only them or then a ReusedQuery.
	 *
	 * @return Null if not only a single {@link CQReusedQuery} was found beside
	 * {@link CQAnd} / {@link CQOr}.
	 */
	public static class SingleReusedChecker implements QueryVisitor {

		final List<CQReusedQuery> reusedElements = new ArrayList<>();
		private boolean containsOthersElements = false;

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQReusedQuery) {
				reusedElements.add((CQReusedQuery) element);
			}
			else if (element instanceof CQAnd || element instanceof CQOr) {
				// Ignore these elements
			}
			else {
				containsOthersElements = true;
			}
		}

		public ManagedExecutionId getOnlyReused() {
			return (reusedElements.size() == 1 && !containsOthersElements) ? reusedElements.get(0).getQuery() : null;
		}
	}


	/**
	 * Find all ReusedQuery in the queries tree, and return their Ids.
	 *
	 */
	public static class AllReusedFinder implements QueryVisitor {

		@Getter
		final List<CQReusedQuery> reusedElements = new ArrayList<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQReusedQuery) {
				reusedElements.add((CQReusedQuery) element);
			}
		}

	}

	/**
	 * Collects all {@link NamespacedId} references provided by a user from a
	 * {@link Visitable}.
	 */
	public static class NamespacedIdCollector implements QueryVisitor {

		@Getter
		private Set<NamespacedId> ids = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof NamespacedIdHolding) {
				NamespacedIdHolding idHolder = (NamespacedIdHolding) element;
				ids.addAll(idHolder.collectNamespacedIds());
			}
		}
	}
}
