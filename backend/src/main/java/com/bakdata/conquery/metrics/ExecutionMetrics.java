package com.bakdata.conquery.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import lombok.Data;

public class ExecutionMetrics {

	private static final String QUERIES = "queries";
	private static final String CONCEPTS = "concepts";
	private static final String CLASSES = "classes";
	private static final String SELECTS = "selects";
	private static final String FILTERS = "filters";
	private static final String RUNNING = "running";
	private static final String STATE = "state";
	private static final String TIME = "time";

	public static Counter getRunningQueriesCounter(String tag) {
		return SharedMetricRegistries.getDefault().counter(nameWithTag(MetricRegistry.name(QUERIES, RUNNING), tag));
	}

	protected static String nameWithTag(String name, String tag) {
		return name + String.format(";tag=%s", tag);
	}

	public static Histogram getQueriesTimeHistogram(String tag) {
		return SharedMetricRegistries.getDefault().histogram(nameWithTag(MetricRegistry.name(QUERIES, TIME), tag));
	}

	public static Counter getQueryStateCounter(ExecutionState state, String tag) {
		return SharedMetricRegistries.getDefault().counter(nameWithTag(MetricRegistry.name(QUERIES, STATE, state.toString()), tag));
	}

	public static void reportQueryClassUsage(Class<? extends QueryDescription> clazz, String tag) {
		SharedMetricRegistries.getDefault()
							  .counter(nameWithTag(MetricRegistry.name(QUERIES, CLASSES, clazz.getSimpleName()), tag))
							  .inc(); // Count usages of different types of Queries
	}

	/**
	 * Report all NamespacedIds to the metrics registry.
	 */
	public static void reportNamespacedIds(Collection<NamespacedId> foundIds, String tag) {
		final Set<ConceptId> reportedIds = new HashSet<>(foundIds.size());

		for (NamespacedId id : foundIds) {
			// We don't want to report the whole tree, as that would be spammy and potentially wrong.

			if (id instanceof ConceptElementId) {
				reportedIds.add(((ConceptElementId<?>) id).findConcept());
			}
			else if (id instanceof ConnectorId) {
				reportedIds.add(((ConnectorId) id).getConcept());
			}
			else if (id instanceof ConceptId) {
				reportedIds.add(((ConceptId) id));
			}
			else if (id instanceof ConceptTreeChildId) {
				reportedIds.add(((ConceptTreeChildId) id).findConcept());
			}
			else if (id instanceof SelectId) {
				reportedIds.add(((SelectId) id).findConcept());
			}
		}

		for (ConceptId id : reportedIds) {
			SharedMetricRegistries.getDefault().counter(nameWithTag(MetricRegistry.name(QUERIES, CONCEPTS, id.toString()), tag)).inc();
		}
	}

	/**
	 * Log the entire Query tree into Metrics
	 */
	@Data
	public static class QueryMetricsReporter implements QueryVisitor {

		private final String tag;

		@Override
		public void accept(Visitable element) {

			if (element instanceof CQElement) {
				SharedMetricRegistries.getDefault()
									  .counter(nameWithTag(MetricRegistry.name(QUERIES, CLASSES, element.getClass().getSimpleName()), getTag()))
									  .inc();
			}

			if (element instanceof CQConcept) {
				for (Select select : ((CQConcept) element).getSelects()) {
					SharedMetricRegistries.getDefault()
										  .counter(nameWithTag(MetricRegistry.name(QUERIES, CLASSES, select.getClass().getSimpleName()), getTag()))
										  .inc();

					SharedMetricRegistries.getDefault()
										  .counter(nameWithTag(MetricRegistry.name(QUERIES, SELECTS, select.getId().toString()), getTag()))
										  .inc();
				}

				// Report classes and ids used of filters and selects
				for (CQTable table : ((CQConcept) element).getTables()) {

					for (FilterValue<?> filter : table.getFilters()) {
						SharedMetricRegistries.getDefault()
											  .counter(nameWithTag(MetricRegistry.name(QUERIES, CLASSES, filter.getFilter().getClass().getSimpleName()), getTag()))
											  .inc();
						SharedMetricRegistries.getDefault()
											  .counter(nameWithTag(MetricRegistry.name(QUERIES, FILTERS, filter.getFilter().getId().toString()), getTag()))
											  .inc();
					}

					for (Select select : table.getSelects()) {
						SharedMetricRegistries.getDefault()
											  .counter(nameWithTag(MetricRegistry.name(QUERIES, CLASSES, select.getClass().getSimpleName()), getTag()))
											  .inc();

						SharedMetricRegistries.getDefault()
											  .counter(nameWithTag(MetricRegistry.name(QUERIES, SELECTS, select.getId().toString()), getTag()))
											  .inc();
					}
				}
			}
		}
	}
}
