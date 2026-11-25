package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "UPDATE_METADATA", base = NamespacedMessage.class)
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@ToString
public class UpdateElementMatchingStats extends NamespaceMessage {
	private final WorkerId source;

	@ToString.Exclude
	private final Map<ConceptElementId<?>, MatchingStats.Entry> values;

	@Override
	public void react(DistributedNamespace context) throws Exception {
		final String sourceString = source.toString();

		for (Entry<ConceptElementId<?>, MatchingStats.Entry> entry : values.entrySet()) {
			try {
				final ConceptElementId<?> element = entry.getKey();
				final MatchingStats.Entry value = entry.getValue();
				final MatchingStats matchingStats = Objects.requireNonNullElseGet(
						context.getStorage().getMatchingStats(element),
						MatchingStats::new
				);
				matchingStats.addEntry(sourceString, value);
				context.getStorage().updateMatchingStats(element, matchingStats);
			} catch (Exception e) {
				log.error("Failed to set matching stats for '{}' (enable TRACE for exception)", entry.getKey(), (Exception) (log.isTraceEnabled() ? e : null));
			}
		}
	}

}
