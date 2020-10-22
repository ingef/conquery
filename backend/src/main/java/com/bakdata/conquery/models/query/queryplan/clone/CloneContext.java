package com.bakdata.conquery.models.query.queryplan.clone;

import java.util.IdentityHashMap;

import com.bakdata.conquery.io.xodus.ModificationShieldedWorkerStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CloneContext {
	@Getter
	private final ModificationShieldedWorkerStorage storage;
	private final IdentityHashMap<CtxCloneable<?>, CtxCloneable<?>> cloneCache = new IdentityHashMap<>();

	/**
	 * Force a clone into the cache (which might not actually be a clone).
	 */
	public <T extends CtxCloneable<? super T>> T inject(T orig, T obj) {

		CtxCloneable<?> old = cloneCache.put(orig, obj);

		if (old != null) {
			throw new IllegalStateException();
		}

		return obj;
	}


	public <T extends CtxCloneable<? super T>> T clone(T obj) {
		CtxCloneable<?> value = cloneCache.get(obj);
		if(value == null) {
			value = obj.doClone(this);
			CtxCloneable<?> old = cloneCache.put(obj, value);
			if(old != null) {
				throw new IllegalStateException("Object was cloned twice");
			}
		}
		return (T) value;
	}
}
