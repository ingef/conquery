package com.bakdata.conquery.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import lombok.Data;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionMetrics {

	private static final String QUERIES = "queries";
	private static final String CONCEPTS = "concepts";
	private static final String CLASSES = "classes";
	private static final String SELECTS = "selects";
	private static final String FILTERS = "filters";
	private static final String RUNNING = "running";
	private static final String STATE = "state";
	private static final String TIME = "time";

	/**
	 * Add group to name.
	 */
	private static String nameWithGroupTag(String name, String group) {
		return String.format("%s.%s", name, group);
	}

	public static Counter getRunningQueriesCounter(String group) {
		return SharedMetricRegistries.getDefault().counter(nameWithGroupTag(MetricRegistry.name(QUERIES, RUNNING), group));
	}


	public static Histogram getQueriesTimeHistogram(String group) {
		return SharedMetricRegistries.getDefault().histogram(nameWithGroupTag(MetricRegistry.name(QUERIES, TIME), group));
	}

	public static Counter getQueryStateCounter(ExecutionState state, String group) {
		return SharedMetricRegistries.getDefault().counter(nameWithGroupTag(MetricRegistry.name(QUERIES, STATE, state.toString()), group));
	}

	public static void reportQueryClassUsage(Class<? extends QueryDescription> clazz, String group) {
		SharedMetricRegistries.getDefault()
							  .counter(nameWithGroupTag(MetricRegistry.name(QUERIES, CLASSES, clazz.getSimpleName()), group))
							  .inc(); // Count usages of different types of Queries
	}

	/**
	 * Report all NamespacedIds to the metrics registry.
	 */
	public static void reportNamespacedIds(Collection<NamespacedIdentifiable<?>> foundIds, String group) {
		final Set<ConceptId> reportedIds = new HashSet<>(foundIds.size());

		for (NamespacedIdentifiable<?> identifiable : foundIds) {

			NamespacedId id = identifiable.getId();
			// We don't want to report the whole tree, as that would be spammy and potentially wrong.

			if (id instanceof ConceptId) {
				reportedIds.add(((ConceptId) id));
			}
			else if (id instanceof ConceptTreeChildId) {
				reportedIds.add(((ConceptTreeChildId) id).findConcept());
			}
			else if (id instanceof ConceptElementId) {
				reportedIds.add(((ConceptElementId<?>) id).findConcept());
			}
			else if (id instanceof ConnectorId) {
				reportedIds.add(((ConnectorId) id).getConcept());
			}
			else if (id instanceof SelectId) {
				reportedIds.add(((SelectId) id).findConcept());
			}
		}

		for (ConceptId id : reportedIds) {
			SharedMetricRegistries.getDefault().counter(nameWithGroupTag(MetricRegistry.name(QUERIES, CONCEPTS, id.toString()), group)).inc();
		}
	}

	/**
	 * Log the entire Query tree into Metrics, every id and class only once.
	 */
	@Data
	public static class QueryMetricsReporter implements QueryVisitor {

		private final String group;

		private final Set<String> reportedMetrics = new HashSet<>();

		@Override
		public void accept(Visitable element) {
			if (element instanceof CQElement) {
				doReport(CLASSES, element.getClass().getSimpleName());
			}

			if (element instanceof CQConcept) {
				for (Select select : ((CQConcept) element).getSelects()) {
					doReport(CLASSES, select.getClass().getSimpleName());
					doReport(SELECTS, select.getId().toString());
				}

				// Report classes and ids used of filters and selects
				for (CQTable table : ((CQConcept) element).getTables()) {

					for (FilterValue<?> filter : table.getFilters()) {
						doReport(CLASSES, filter.getFilter().getClass().getSimpleName());
						doReport(FILTERS, filter.getFilter().getId().toString());
					}

					for (Select select : table.getSelects()) {
						doReport(CLASSES, select.getClass().getSimpleName());

						doReport(SELECTS, select.getId().toString());
					}
				}
			}
		}


		/**
		 * Ensure that metrics are only reported once.
		 */
		public void doReport(String category, String id) {
			final String name = nameWithGroupTag(MetricRegistry.name(QUERIES, category, id), getGroup());

			if (!reportedMetrics.add(name)) {
				return;
			}

			SharedMetricRegistries.getDefault()
								  .counter(name)
								  .inc();
		}
	}
}
