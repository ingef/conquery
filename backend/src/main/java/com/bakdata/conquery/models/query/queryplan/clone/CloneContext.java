package com.bakdata.conquery.models.query.queryplan.clone;

import java.util.IdentityHashMap;

public class CloneContext {
	private final IdentityHashMap<CtxCloneable<?>, CtxCloneable<?>> cloneCache = new IdentityHashMap<>();
	
	public <T extends CtxCloneable<? super T>> T clone(T obj) {
		CtxCloneable<?> value = cloneCache.get(obj);
		if(value == null) {
			value = obj.doClone(this);
			CtxCloneable<?> old = cloneCache.put(obj, value);
			if(old != null) {
				throw new IllegalStateException();
			}
		}
		return (T) value;
	}
}
