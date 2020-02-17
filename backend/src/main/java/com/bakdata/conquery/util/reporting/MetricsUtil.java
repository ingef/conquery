package com.bakdata.conquery.util.reporting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.codahale.metrics.MetricRegistry;
import lombok.NonNull;

public class MetricsUtil {
	/**
	 * Report all NamespacedIds to the metrics registry.
	 */
	public static void reportNamespacedIds(Collection<NamespacedId> foundIds, User user, MetricRegistry metricRegistry, @NonNull MasterMetaStorage storage) {
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
			metricRegistry.counter(primaryGroup + ".queries.content." + id.toString()).inc();
		}
	}
}
