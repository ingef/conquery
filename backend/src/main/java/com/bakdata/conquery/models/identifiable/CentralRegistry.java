package com.bakdata.conquery.models.identifiable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationResolveError;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"}) @NoArgsConstructor
public class CentralRegistry implements Injectable {
	
	private final IdMap map = new IdMap<>();
	private final ConcurrentMap<IId<?>, Function<IId,Identifiable>> cacheables = new ConcurrentHashMap<>();
	
	public synchronized void register(Identifiable<?> ident) {
		map.add(ident);
	}
	
	public synchronized void registerCacheable(IId id, Function<IId,Identifiable> supplier) {
		cacheables.put(id, supplier);
	}

	protected  <T extends Identifiable<?>> T get(IId<T> name) {
		Object res = map.get(name);
		if (res != null) {
			return (T) res;
		}

		Function<IId, Identifiable> supplier = cacheables.get(name);
		if (supplier == null) {
			return null;
		}
		return (T) supplier.apply(name);
	}

	public <T extends Identifiable<?>> T resolve(IId<T> name) {
		final T result = get(name);

		if(result == null){
			throw new ExecutionCreationResolveError(name);
		}

		return result;
	}

	public <T extends Identifiable<?>> Optional<T> getOptional(IId<T> name) {
		return Optional.ofNullable(get(name));
	}

	public void remove(Identifiable<?> ident) {
		IId<?> id = ident.getId();
		synchronized (this) {
			map.remove(id);
			cacheables.remove(id);
		}
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(CentralRegistry.class, this);
	}

	public static CentralRegistry get(DeserializationContext ctxt) throws JsonMappingException {
		CentralRegistry result = (CentralRegistry) ctxt.findInjectableValue(CentralRegistry.class.getName(), null, null);
		if(result == null) {
			IdResolveContext alternative = (IdResolveContext)ctxt.findInjectableValue(IdResolveContext.class.getName(), null, null);
			if(alternative == null) {
				return null;
			}
			return alternative.getMetaRegistry();
		}
		return result;
	}

	public void clear() {
		map.clear();
		cacheables.clear();
	}
}
