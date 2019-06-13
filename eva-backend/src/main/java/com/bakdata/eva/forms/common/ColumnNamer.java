package com.bakdata.eva.forms.common;

import java.util.ArrayList;
import java.util.function.Function;

import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import com.bakdata.conquery.models.query.concept.SelectDescriptor;

public class ColumnNamer{
	private Integer counter = 0;
	
	public Function<SelectDescriptor, String> getNamer() {
		return sd -> {
			ArrayList<ConceptElementId<?>> nodes = new ArrayList<>(sd.getCqConcept().getIds());
			nodes.sort(Form.CEID_COMPARATOR);
			String str = nodes.get(0).getName()+ "_" + (counter++);
			// Minuses need to be replaced by underscores because the REnd would interpret it as the end of the name
			return str.replace('-', '_');
		};
	}
}