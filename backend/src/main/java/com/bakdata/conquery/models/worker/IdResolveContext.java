package com.bakdata.conquery.models.worker;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.dropwizard.jackson.Jackson;

/**
 * Superclass for implementations that map ids to existing objects in the conquery id system.
 * This is a bridge between {@link Jackson} and conquery id serdes.
 */
public abstract class IdResolveContext implements Injectable {

	public static IdResolveContext get(DeserializationContext ctxt) throws JsonMappingException {
		IdResolveContext namespaces = (IdResolveContext) ctxt
				.findInjectableValue(IdResolveContext.class.getName(), null, null);
		if(namespaces == null) {
			throw new NoSuchElementException("Could not find injected namespaces");
		}
		return namespaces;
	}
	
	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(IdResolveContext.class, this);
	}

	public abstract CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException;
	@JsonIgnore
	public abstract CentralRegistry getMetaRegistry();

	public <ID extends AId<T> & NamespacedId, T extends Identifiable<?>> T resolve(ID id) {
		return findRegistry(id.getDataset()).resolve(id);
	}

	public <ID extends AId<T> & NamespacedId, T extends Identifiable<?>> Optional<T> getOptional(ID id) {
		return findRegistry(id.getDataset()).getOptional(id);
	}

	public <ID extends AId<T> & NamespacedId, T extends Identifiable<?>> Optional<T> getOptional(DatasetId dataset, ID id) {
		return findRegistry(dataset).getOptional(id);
	}
}
