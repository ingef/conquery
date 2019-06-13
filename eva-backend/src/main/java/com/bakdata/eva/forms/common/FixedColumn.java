package com.bakdata.eva.forms.common;

import java.util.Arrays;
import java.util.function.Consumer;

import com.bakdata.conquery.models.concepts.ConceptElement;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.concepts.virtual.VirtualConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.models.forms.FeatureGroup;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@RequiredArgsConstructor(access=AccessLevel.PRIVATE)
public class FixedColumn {
	@Getter
	private final FeatureGroup occurringGroup;
	@Getter
	private final ConceptElementId<?> key;
	@Getter @Accessors(chain=true) @Setter
	private Consumer<CQConcept> conceptConfiguration = concept -> {};
	
	public static FixedColumn of(FeatureGroup occurringGroup, String... hierarchicalNameParts) {
		FixedColumn f = new FixedColumn(occurringGroup, ConceptElementId.Parser.INSTANCE.parse(Arrays.asList(hierarchicalNameParts)));
		return f;
	}

	public ConceptElement<?> resolve(Namespaces namespaces) {
		ConceptElement<?> element = namespaces.get(key.getDataset())
			.getStorage().getCentralRegistry()
			.getOptional(key.findConcept())
			.map(concept -> concept.getElementById(key))
			.orElse(null);
		if(element==null)
			throw new IllegalStateException("Could not resolve the fixed column "+key);
		if(!(element instanceof ConceptTreeChild || element instanceof VirtualConcept)) {
			throw new IllegalStateException("The fixed column "+key+" is not a valid target (must be virtual or non-root).");
		}
		return element;
	}
}
