package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
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
        // We collect the concepts outside the loop to update the storage afterward
        Map<ConceptId, Concept<?>> conceptsToUpdate = new HashMap<>();

        for (Entry<ConceptElementId<?>, MatchingStats.Entry> entry : values.entrySet()) {
            try {
                ConceptElementId<?> element = entry.getKey();
                ConceptId conceptId = element.findConcept();

                // mapping function cannot use Id::resolve here yet, somehow the nsIdResolver is not set because it
                // stems from a map key. Jackson seems to use a different serializer.
                Concept<?> concept = conceptsToUpdate.computeIfAbsent(conceptId, id -> context.getStorage().getConcept(id));

                final ConceptElement<?> target = concept.findById(element);

                final MatchingStats.Entry value = entry.getValue();

                conceptsToUpdate.put(conceptId, concept);

                MatchingStats matchingStats = target.getMatchingStats();
                if (matchingStats == null) {
                    matchingStats = new MatchingStats();
                    target.setMatchingStats(matchingStats);
                }
                matchingStats.putEntry(source, value);
            } catch (Exception e) {
                log.error("Failed to set matching stats for '{}' (enable TRACE for exception)", entry.getKey(), (Exception) (log.isTraceEnabled() ? e : null));
            }
        }

        conceptsToUpdate.values().forEach(context.getStorage()::updateConcept);
    }

}
