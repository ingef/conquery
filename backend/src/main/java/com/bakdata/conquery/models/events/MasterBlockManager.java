package com.bakdata.conquery.models.events;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MasterBlockManager implements BlockManager {

	private final NamespaceStorage storage;
	
	@Override
	public void addImport(Import imp) {
		for(Concept<?> c: storage.getAllConcepts()) {
			for(Connector con : c.getConnectors()) {
				if(con.getTable().getId().equals(imp.getTable())) {
					c.getMatchingStats().addImport(imp);
				}
			}
		}
	}
	
	@Override
	public void addConcept(Concept<?> c) {
		for(Import imp: storage.getAllImports()) {
			for(Connector con : c.getConnectors()) {
				if(con.getTable().getId().equals(imp.getTable())) {
					c.getMatchingStats().addImport(imp);
				}
			}
		}
	}

	@Override
	public void removeImport(ImportId imp) {
		for(Concept<?> c: storage.getAllConcepts()) {
			for(Connector con : c.getConnectors()) {
				if(con.getTable().getId().equals(imp.getTable())) {
					c.getMatchingStats().removeImport(imp);
				}
			}
		}
	}
	
}
