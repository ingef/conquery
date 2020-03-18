package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Map;
import java.util.Map.Entry;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.MatchingStats;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptTreeChildId;
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
	private final Map<ConceptElementId<?>, MatchingStats.Entry> values;

	@Override
	public void react(Namespace context) throws Exception {
		for(Entry<ConceptElementId<?>, MatchingStats.Entry> entry : values.entrySet()) {
			try {
				ConceptElementId<?> target = entry.getKey();
				MatchingStats.Entry value = entry.getValue();

				Concept<?> c = context.getStorage().getConcept(target.findConcept());
				//if a child node
				if(target instanceof ConceptTreeChildId) {
					ConceptTreeNode<?> child = c.getChildById((ConceptTreeChildId) target);
					child.getMatchingStats().updateEntry(source, value);
				}
				//otherwise just update the concept
				else {
					c.getMatchingStats().updateEntry(source, value);
				}
			}
			catch(Exception e) {
				log.error("Failed to set matching stats for '{}'", entry.getKey());
			}
		}
	}
	
}
