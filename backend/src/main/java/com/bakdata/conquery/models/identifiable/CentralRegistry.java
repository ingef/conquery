package com.bakdata.conquery.models.identifiable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationResolveError;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
@NoArgsConstructor
@ToString(of = "map")
public class CentralRegistry implements Injectable {

	private final IdMap map = new IdMap<>();
	private final ConcurrentMap<Id<?>, Function<Id, Identifiable>> cacheables = new ConcurrentHashMap<>();

	public synchronized CentralRegistry register(Identifiable<?> ident) {
		map.add(ident);
		return this;
	}

	public synchronized Function<Id, Identifiable> registerCacheable(Id id, Function<Id, Identifiable> supplier) {
		return cacheables.put(id, supplier);
	}

	public <T extends Identifiable<?>> T resolve(Id<T> name) {
		final T result = get(name);

		if (result == null) {
			throw new ExecutionCreationResolveError(name);
		}

		return result;
	}

	public Identifiable update(Identifiable<?> ident) {
		return map.update(ident);
	}

	public synchronized Optional<Identifiable> updateCacheable(Id id, Function<Id, Identifiable> supplier) {
		Function<Id, Identifiable> old = cacheables.put(id, supplier);
		if (old != null) {
			// If the cacheable was still there, the Object was never cached.
			return Optional.empty();
		}
		// The supplier might have been invoked already and the object gone into the IdMap
		// So we invalidate it
		return Optional.ofNullable(map.remove(id));
	}

	public <T extends Identifiable<?>> Optional<T> getOptional(Id<T> name) {
		return Optional.ofNullable(get(name));
	}

	public synchronized void remove(Identifiable<?> ident) {
		Id<?> id = ident.getId();
		map.remove(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(CentralRegistry.class, this)
					 // Possibly overriding mapping for DatasetRegistry
					 .add(IdResolveContext.class, new SingletonNamespaceCollection(this));
	}

	public static CentralRegistry get(DeserializationContext ctxt) throws JsonMappingException {
		CentralRegistry result = (CentralRegistry) ctxt.findInjectableValue(CentralRegistry.class.getName(), null, null);
		if (result != null) {
			return result;
		}

		IdResolveContext alternative = (IdResolveContext) ctxt.findInjectableValue(IdResolveContext.class.getName(), null, null);
		if (alternative == null) {
			return null;
		}
		return alternative.getMetaRegistry();
	}

	public void clear() {
		map.clear();
		cacheables.clear();
	}

	/**
	 * Needs to be protected in order to be overwritten by {@link InjectingCentralRegistry}
	 */
	protected <T extends Identifiable<?>> T get(Id<T> name) {
		Object res = map.get(name);
		if (res != null) {
			return (T) res;
		}
		synchronized (this) {
			// Retry synchronized to make sure it has not been resolved from cacheables in the mean time
			Object res2 = map.get(name);
			if (res2 != null) {
				return (T) res2;
			}
			Function<Id, Identifiable> supplier = cacheables.get(name);
			if (supplier == null) {
				return null;
			}

			// Transfer object to the IdMap
			final T apply = (T) supplier.apply(name);
			register(apply);
			cacheables.remove(name);
		}

		return (T) map.get(name);
	}

}
