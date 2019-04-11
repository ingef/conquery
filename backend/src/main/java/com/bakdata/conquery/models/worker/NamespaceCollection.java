package com.bakdata.conquery.models.worker;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface NamespaceCollection extends Injectable {

	static NamespaceCollection get(DeserializationContext ctxt) throws JsonMappingException {
		NamespaceCollection namespaces = (NamespaceCollection) ctxt
				.findInjectableValue(NamespaceCollection.class.getName(), null, null);
		if(namespaces == null) {
			throw new NoSuchElementException("Could not find injected namespaces");
		}
		else {
			return namespaces;
		}
	}
	
	@Override
	default MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NamespaceCollection.class, this);
	}
	
	CentralRegistry findRegistry(DatasetId dataset);
	@JsonIgnore
	CentralRegistry getMetaRegistry();

	default <ID extends NamespacedId&IId<T>, T extends Identifiable<?>> T resolve(ID id) {
		return findRegistry(id.getDataset()).resolve(id);
	}
	
	default <ID extends NamespacedId&IId<T>, T extends Identifiable<?>> Optional<T> getOptional(ID id) {
		return findRegistry(id.getDataset()).getOptional(id);
	}
}
