package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Map;
import java.util.Map.Entry;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefKeys;
import com.bakdata.conquery.models.datasets.concepts.ConceptElement;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id="UPDATE_METADATA", base=NamespacedMessage.class)
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @ToString
public class UpdateElementMatchingStats extends NamespaceMessage.Slow {
	private final WorkerId source;

	@ToString.Exclude
	@NsIdRefKeys
	private final Map<ConceptElement<?>, MatchingStats.Entry> values;

	@Override
	public void react(Namespace context) throws Exception {
		for(Entry<ConceptElement<?>, MatchingStats.Entry> entry : values.entrySet()) {
			try {
				ConceptElement<?> target = entry.getKey();
				MatchingStats.Entry value = entry.getValue();

				target.getMatchingStats().updateEntry(source, value);
			}
			catch(Exception e) {
				log.error("Failed to set matching stats for '{}'", entry.getKey());
			}
		}
	}
	
}
