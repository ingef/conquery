package com.bakdata.conquery.models.identifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.identifiable.ids.IId;

import lombok.NoArgsConstructor;

//see #154  Why is this not a Map aswell?

@NoArgsConstructor
public class IdMap<ID extends IId<? extends V>, V extends Identifiable<? extends ID>> implements Set<V> {

	private Map<ID, V> map;
	@NotNull @Valid
	private final List<V> list = new ArrayList<>();
	
	public IdMap(Collection<? extends V> c) {
		for(V e:c) {
			add(e);
		}
	}
	
	public Collection<V> values() {
		return Collections.unmodifiableList(list);
	}
	
	@Override
	public Iterator<V> iterator() {
		return values().iterator();
	}

	@Override
	public Stream<V> stream() {
		return list.stream();
	}
	
	@Override
	public int size() {
		return list.size();
	}

	public V getOrFail(ID id) {
		initMap();
		V res = map.get(id);
		if(res==null) {
			throw new NoSuchElementException("Could not find an element called '"+id+"'");
		}
		return res;
	}
	
	public void initMap() {
		if(map==null) {
			map = new HashMap<>();
			for(V e:list) {
				addToMap(e);
			}
		}
	}

	private void addToMap(V e) {
		V old = map.put(e.getId(), e);
		if(old != null && old !=e) {
			throw new IllegalStateException("The element "+e.getId()+" is present twice in this map.");
		}
	}

	public Optional<V> getOptional(ID id) {
		initMap();
		return Optional.ofNullable(map.get(id));
	}
	
	@Override
	public boolean add(V e) {
		list.add(e);
		if(map!=null) {
			addToMap(e);
		}
		return true;
	}
	
	public boolean update(V e) {
		initMap();
		V oldValue = map.put(e.getId(), e);
		if(oldValue!=null) {
			list.remove(e);
		}
		list.add(e);
		return true;
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}
	
	public V remove(ID id) {
		initMap();
		V obj = map.remove(id);
		if(obj!=null) {
			list.remove(obj);
		}
		return obj;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
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
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
