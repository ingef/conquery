package com.bakdata.conquery.models.concepts.virtual;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.SelectHolder;
import com.bakdata.conquery.models.concepts.select.Select;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

/**
 * This is a single node or concept in a concept tree.
 */
@CPSType(id="VIRTUAL", base=Concept.class)
public class VirtualConcept extends Concept<VirtualConceptConnector> implements SelectHolder<Select> {
	
	@NotNull @Getter @Setter @JsonManagedReference
	private List<Select> selects = new ArrayList<>();

	@Override
	public Concept<?> findConcept() {
		return getConcept();
	}
}
