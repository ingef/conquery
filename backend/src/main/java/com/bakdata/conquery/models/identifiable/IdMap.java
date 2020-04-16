package com.bakdata.conquery.models.identifiable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.collect.ForwardingMap;

public class IdMap<ID extends IId<? super V>, V extends Identifiable<? extends ID>> extends ForwardingMap <ID ,V > implements Iterable<V>{

	private final ConcurrentMap<ID, V> map;
	
	public IdMap() {
		map = new ConcurrentHashMap<ID, V>();
	}
	
	public IdMap(Collection<V> collection) {
		map = new ConcurrentHashMap<>();
		for(V value : collection) {
			map.put(value.getId(), value);
		}
	}
	
	@Override
	public Set<Map.Entry <ID, V>> entrySet(){
		return map.entrySet();
	}
	
	@Override
	public Collection<V> values() {
		return map.values();
	}
	
	@Override
	public Iterator<V> iterator() {
		return values().iterator();
	}

	public Stream<V> stream() {
		return map.values().stream();
	}
	
	@Override
	public int size() {
		return map.values().size();
	}

	public V getOrFail(ID id) {
		V res = map.get(id);
		if(res==null) {
			throw new NoSuchElementException("Could not find an element called '"+id+"'");
		}
		return res;
	}

	private void addToMap(V entry) {
		V old = map.put(entry.getId(), entry);
		if(old != null && !old.equals(entry)) {
			throw new IllegalStateException("The element "+entry.getId()+" is present twice in this map.");
		}
	}

	public Optional<V> getOptional(ID id) {
		return Optional.ofNullable(map.get(id));
	}
	
	public boolean add(V entry) {
		addToMap(entry);
		return true;
	}
	
	public void update(V entry) {
		map.put(entry.getId(), entry);
	}
	
	public V remove(ID id) {
		V obj = map.remove(id);
		return obj;
	}
	
	@Override @Deprecated
	public V remove(Object object) {
		return super.remove(object);
	}
	
	@Override @Deprecated
	public V put(ID key, V value) {
		return super.put(key, value);
	}
	
	public boolean addAll(Collection<? extends V> c) {
		boolean modified = false;
		for (V e : c) {
			if (add(e)) {
				modified = true;
			}
		}
		return modified;
	}

	@Override
	protected Map <ID, V> delegate() {
		return map;
	}
}
