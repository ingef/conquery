package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.concept.filter.CQTable;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.Namespaces;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Sets {@link Select}s on a {@link CQConcept} that are flaged with default=true depending on the {@link FillMethod}.
 */
@NoArgsConstructor
@AllArgsConstructor
public class DefaultSelectConceptManipulator implements ConceptManipulator {
	public static enum FillMethod {
		OVERWRITE,				// Overwrite all present selects with the default value.
		ADD,					// Add default selects to all present selects.
		ADD_TO_COMPLETE_EMPTY	// Add default selects only if no selects were present in the whole concept.
		};
		
	private  FillMethod method = FillMethod.ADD_TO_COMPLETE_EMPTY;

	@Override
	public void consume(CQConcept concept, Namespaces namespaces) {
		// Obtain the concept Id
		List<ConceptElementId<?>> conceptIds = concept.getIds();
		if(conceptIds.isEmpty()) {
			throw new IllegalArgumentException(String.format("Cannot set defaults on a CQConcept without ids. Provided concept: %s", concept));
		}
		
		// Gather Default Selects
			// On concept level
		Concept<?> actualConcept = namespaces.resolve(concept.getIds().get(0).findConcept());
		List<Select> defaultConceptSelects = new ArrayList<>(actualConcept.getSelects());
		defaultConceptSelects.removeIf(s -> !s.isDefault());
		
			// On connector level
		Map<CQTable, List<Select>> tableSelects = new HashMap<>();
		for (CQTable table : concept.getTables()) {
			Connector connector = namespaces.resolve(table.getId());
			tableSelects.put(table, connector.getSelects().stream().filter(Select::isDefault).collect(Collectors.toList()));
		}
		

		// Put selects into concept
		switch(method) {
			
			case ADD_TO_COMPLETE_EMPTY:
				Boolean allTablesEmpty = concept.getTables().stream()
					.map(CQTable::getSelects)
					.map(List::isEmpty)
					.reduce(Boolean::logicalAnd)
					.orElse(true /* No table present -> signal empty tables*/);
				if(!(concept.getSelects().isEmpty() && allTablesEmpty)) {
					// Don't fill if there are any selects on concept level or on any table level
					break;
				}
				// Intended fall-through to ADD here !!!
				
			case ADD:
				// TODO Add select only if it is not present already
				ArrayList<Select> cSelects = new ArrayList<>(concept.getSelects());
				cSelects.addAll(defaultConceptSelects);
				concept.setSelects(cSelects);
				concept.getTables().forEach(t -> {

					ArrayList<Select> conSelects = new ArrayList<>(t.getSelects());
					conSelects.addAll(tableSelects.get(t));
					t.setSelects(conSelects);
					
				});
				break;
				
			case OVERWRITE:
				concept.setSelects(defaultConceptSelects);
				concept.getTables().forEach(t -> t.setSelects(tableSelects.get(t)));
				break;
			default:
				throw new IllegalStateException(String.format("Unknown fill method %s while filling concept %s.", method, concept));
		}
	}

}
