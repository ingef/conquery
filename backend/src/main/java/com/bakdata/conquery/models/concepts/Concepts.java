package com.bakdata.conquery.models.concepts;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptsId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class wraps all the root concepts
 */
@Slf4j //FIXME refactor away
public class Concepts extends IdentifiableImpl<ConceptsId> {
	
	@JsonBackReference @Getter @Setter
	private Dataset dataset;
	@JsonIgnore @Valid @Getter @Setter
	private transient List<Concept<?>> concepts = new ArrayList<>();
	@Valid @Getter @Setter
	private List<StructureNode> structureRoots = new ArrayList<>();
	@JsonIgnore @Getter
	private List<StructureNode> allStructureNodes = new ArrayList<>();
	@JsonIgnore @Getter
	private final IdMap<ConnectorId, Connector> allConnectors = new IdMap<>();
	/*
	public void consolidate(Dataset dataset, BiConsumer<Class<?>,IdentifiableImpl> nodeConsumer, Validator validator) throws ConfigurationException {
		
		//TODO load concepts anew from storage
		
		for(Concept<?> concept:concepts) {
			
		}
		
		//resolve structure nodes
		Queue<StructureNode> stOpen = new ArrayDeque<>(structureRoots);
		while(!stOpen.isEmpty()) {
			StructureNode n = stOpen.poll();
			nodeConsumer.accept(StructureNode.class, n);
			allStructureNodes.add(n);
			stOpen.addAll(n.getChildren());
			n.setResolvedContained(
				n.getContainedRoots()
				.stream()
				.map(name -> {
					Optional<Concept<?>> concept = concepts.stream().filter(r->r.getName().equals(name)).findAny();
					if(concept.isPresent())
						return concept.get();
					else {
						log.warn("Could not find the concept tree root '{}'", name);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.peek(r -> r.setStructureParent(n))
				.collect(Collectors.toList())
			);
				
		}
	}*/
	
	public Connector getConnector(ConnectorId indexd) {
		return allConnectors.getOrFail(indexd);
	}

	@Override
	public ConceptsId createId() {
		return new ConceptsId(dataset.getId());
	}

	public void addConcept(Concept<?> concept) throws ConfigurationException, JSONException {
		concepts.add(concept);
	}
}
