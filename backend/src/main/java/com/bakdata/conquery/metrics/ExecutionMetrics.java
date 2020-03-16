package com.bakdata.conquery.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import lombok.NonNull;

public class ExecutionMetrics {
	public static Counter getRunningQueriesCounter() {
		return SharedMetricRegistries.getDefault().counter("queries.running");
	}

	public static Histogram getQueriesTimeHistogram() {
		return SharedMetricRegistries.getDefault().histogram("queries.time");
	}

	public static Counter getQueryStateCounter(ExecutionState state) {
		return SharedMetricRegistries.getDefault().counter("queries.state." + state);
	}

	public static void reportQueryClassUsage(Class<? extends QueryDescription> aClass) {
		SharedMetricRegistries.getDefault().counter(MetricRegistry.name(aClass)).inc(); // Count usages of different types of Queries
	}

	/**
	 * Report all NamespacedIds to the metrics registry.
	 */
	public static void reportNamespacedIds(Collection<NamespacedId> foundIds, User user, @NonNull MasterMetaStorage storage) {
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

		final GroupId primaryGroup = AuthorizationHelper.getPrimaryGroup(user, storage).getId();

		for (ConceptId id : reportedIds) {
			SharedMetricRegistries.getDefault().counter(primaryGroup + ".queries.content." + id.toString()).inc();
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
									 .forEach(select -> SharedMetricRegistries.getDefault().counter(MetricRegistry.name(select.getClass().getSimpleName(), "usage")).inc());

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
}
