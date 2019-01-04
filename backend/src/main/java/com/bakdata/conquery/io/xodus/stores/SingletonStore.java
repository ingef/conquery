package com.bakdata.conquery.io.xodus.stores;

import com.bakdata.conquery.models.exceptions.JSONException;

public class SingletonStore<VALUE> extends KeyIncludingStore<Boolean, VALUE> {

	public SingletonStore(Store<Boolean, VALUE> store) {
		super(store);
		fillCache();
	}

	@Override
	protected Boolean extractKey(VALUE value) {
		return Boolean.TRUE;
	}

	@Override @Deprecated
	public VALUE get(Boolean key) {
		return get();
	}
	
	public synchronized VALUE get() {
		return super.get(Boolean.TRUE);
	}
	
	@Override @Deprecated
	public void remove(Boolean key) {
		remove();
	}
	
	public synchronized void remove() {
		VALUE v = get();
		super.remove(Boolean.TRUE);
		onValueRemoved(v);
	}
	
	@Override
	public synchronized void update(VALUE value) throws JSONException {
		VALUE old = get();
		if(old != null)
			onValueRemoved(old);
		super.update(value);
		onValueAdded(value);
	}
	
	@Override
	public void fillCache() {
		super.fillCache();
		if(get() != null)
			onValueAdded(get());
	}
	
	protected void onValueAdded(VALUE v) {}
	
	protected void onValueRemoved(VALUE v) {}
}
